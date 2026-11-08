package mips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Asignación de registros de una función: implementación de {@code getReg()}.
 *
 * <p>Reparto de registros:
 * <ul>
 *   <li><b>$s0–$s7 (guardados):</b> contienen los temporales del TAC (t0, t1…). Sobreviven a las llamadas porque
 *       cada función guarda en su prologue los $s que usa y los restaura al salir.</li>
 *   <li><b>$t0–$t7 (temporales):</b> registros de trabajo de una sola instrucción: constantes, variables que se
 *       cargan desde el registro de activación y direcciones. Se liberan al terminar la instrucción.</li>
 *   <li><b>$t8, $t9:</b> reservados para calcular direcciones y para las rutinas del runtime.</li>
 *   <li><b>Pila:</b> cuando los 8 registros $s están ocupados, {@link #getReg} derrama (spill) el temporal cuyo
 *       próximo uso está más lejano a su ranura del registro de activación, y lo recarga cuando se vuelve a leer.</li>
 * </ul>
 *
 * <p>Descriptores (como en el algoritmo clásico): el <i>descriptor de registro</i> dice qué temporal contiene cada
 * $s, y el <i>descriptor de dirección</i> está implícito: un temporal está en su registro, o (si no) en su ranura
 * de la pila. Un registro «sucio» tiene un valor más nuevo que el de la ranura.
 */
final class RegisterAllocator {
    static final String[] S = {"$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"};
    static final String[] T = {"$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7"};
    static final int INF = Integer.MAX_VALUE;

    private final Consumer<String> emit;
    private final IntSupplier curIndex;
    private final List<TacInstr> code;
    private final int slotBase;                       // fp + slotBase = ranura del temporal 0

    private final String[] sTemp = new String[S.length];
    private final boolean[] sDirty = new boolean[S.length];
    private final boolean[] pinned = new boolean[S.length];
    private final Map<String, Integer> tempReg = new HashMap<>();
    private final boolean[] tUsed = new boolean[T.length];
    private int usedMask = 0;

    /** Estadísticas para el reporte y las pruebas. */
    int spills = 0;
    int reloads = 0;

    RegisterAllocator(List<TacInstr> code, int slotBase, Consumer<String> emit, IntSupplier curIndex) {
        this.code = code;
        this.slotBase = slotBase;
        this.emit = emit;
        this.curIndex = curIndex;
    }

    /** Registros $s que usó la función (máscara de bits), para guardarlos en el prologue. */
    int usedSMask() { return usedMask; }

    private int slotOffset(String temp) {
        return slotBase + 4 * Integer.parseInt(temp.substring(1));
    }

    // ------------------------------------------------------------------ temporales ($s)

    /** getReg para leer un temporal: devuelve el registro que contiene su valor (cargándolo de la pila si hace falta). */
    String useTemp(String temp) {
        return getReg(temp, true);
    }

    /** getReg para definir un temporal: devuelve el registro donde se escribirá el nuevo valor (queda sucio). */
    String defTemp(String temp) {
        String r = getReg(temp, false);
        sDirty[tempReg.get(temp)] = true;
        return r;
    }

    /** Marca el temporal como modificado (su registro tiene un valor más nuevo que la ranura). */
    void markDirty(String temp) {
        Integer r = tempReg.get(temp);
        if (r != null) sDirty[r] = true;
    }

    /**
     * getReg(temp): registro para un temporal.
     * <ol>
     *   <li>Si ya está en un registro, se reutiliza.</li>
     *   <li>Si no, se busca un $s libre.</li>
     *   <li>Si no hay ninguno, se derrama el temporal con el próximo uso más lejano (que no esté en uso en esta instrucción).</li>
     *   <li>Si se va a leer y su valor estaba en la pila, se recarga.</li>
     * </ol>
     */
    String getReg(String temp, boolean needValue) {
        Integer cur = tempReg.get(temp);
        if (cur != null) {
            pinned[cur] = true;
            return S[cur];
        }
        int r = -1;
        for (int i = 0; i < S.length; i++) {
            if (sTemp[i] == null) { r = i; break; }
        }
        if (r < 0) r = spillVictim();
        tempReg.put(temp, r);
        sTemp[r] = temp;
        sDirty[r] = false;
        pinned[r] = true;
        usedMask |= 1 << r;
        if (needValue) {
            emit.accept("lw " + S[r] + ", " + slotOffset(temp) + "($fp)      # recarga " + temp + " desde la pila");
            reloads++;
        }
        return S[r];
    }

    /** Elige el registro cuyo temporal se usará más tarde (o nunca), lo guarda en la pila y lo libera. */
    private int spillVictim() {
        int best = -1, bestUse = -1;
        int here = curIndex.getAsInt();
        for (int i = 0; i < S.length; i++) {
            if (pinned[i] || sTemp[i] == null) continue;
            int nu = nextUse(sTemp[i], here);
            if (nu > bestUse) { bestUse = nu; best = i; }
        }
        if (best < 0) throw new IllegalStateException("No hay registros $s para derramar (demasiados operandos en una instrucción)");
        spillReg(best);
        return best;
    }

    private void spillReg(int r) {
        String temp = sTemp[r];
        if (sDirty[r]) {
            emit.accept("sw " + S[r] + ", " + slotOffset(temp) + "($fp)      # derrama " + temp + " a la pila");
            spills++;
        }
        tempReg.remove(temp);
        sTemp[r] = null;
        sDirty[r] = false;
    }

    /** Próxima instrucción posterior a idx que lee el temporal (INF si lo redefine antes o nunca lo usa). */
    int nextUse(String temp, int idx) {
        for (int j = idx + 1; j < code.size(); j++) {
            TacInstr in = code.get(j);
            if (in.reads().contains(temp)) return j;
            if (temp.equals(in.writes())) return INF;
        }
        return INF;
    }

    /** Guarda en la pila los temporales sucios (antes de saltos y etiquetas). Los registros siguen ocupados. */
    void flushDirty() {
        for (int i = 0; i < S.length; i++) {
            if (sTemp[i] != null && sDirty[i]) {
                emit.accept("sw " + S[i] + ", " + slotOffset(sTemp[i]) + "($fp)      # guarda " + sTemp[i] + " en la pila");
                sDirty[i] = false;
            }
        }
    }

    /** Olvida qué hay en los registros (tras un salto incondicional o en una etiqueta); los valores ya están en la pila. */
    void clear() {
        for (int i = 0; i < S.length; i++) {
            sTemp[i] = null;
            sDirty[i] = false;
            pinned[i] = false;
        }
        tempReg.clear();
    }

    // ------------------------------------------------------------------ registros de trabajo ($t)

    /** Reserva un registro $t para esta instrucción. */
    String scratch() {
        for (int i = 0; i < T.length; i++) {
            if (!tUsed[i]) { tUsed[i] = true; return T[i]; }
        }
        throw new IllegalStateException("No hay registros $t libres en la instrucción");
    }

    /** Fin de instrucción: libera los $t, desfija los $s y libera los temporales que ya no se usan. */
    void endInstruction() {
        for (int i = 0; i < T.length; i++) tUsed[i] = false;
        for (int i = 0; i < S.length; i++) pinned[i] = false;
        int here = curIndex.getAsInt();
        for (int i = 0; i < S.length; i++) {
            // solo se descartan los registros limpios: su valor sigue en la pila si se vuelve a necesitar
            if (sTemp[i] != null && !sDirty[i] && nextUse(sTemp[i], here) == INF) {
                tempReg.remove(sTemp[i]);
                sTemp[i] = null;
            }
        }
    }

    /** Temporales que hoy ocupan registros (para el reporte/depuración). */
    Set<String> resident() {
        return new HashSet<>(tempReg.keySet());
    }

    List<String> usedSNames() {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < S.length; i++) if ((usedMask & (1 << i)) != 0) l.add(S[i]);
        return l;
    }
}
