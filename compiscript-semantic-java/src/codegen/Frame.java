package codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registro de activación (marco) de una función, método o del código de nivel superior ("main").
 *
 * Disposición (offsets respecto a fp, en bytes):
 *   0 dirección de retorno | 4 enlace de control | 8 enlace de acceso | 12 valor de retorno
 *   16.. parámetros, luego variables locales (y ocultas), luego temporales.
 */
public class Frame {
    public static final int HEADER = 16;

    public final String label;
    public final int level;
    public final Frame parent;
    public boolean hasThis = false;
    public final List<String> code = new ArrayList<>();

    /** Entradas (nombre, offset, tamaño, clase) para el reporte del registro de activación. */
    public static final class Slot {
        public final String name, kind;
        public final int offset, size;
        Slot(String name, String kind, int offset, int size) {
            this.name = name; this.kind = kind; this.offset = offset; this.size = size;
        }
    }

    public final List<Slot> slots = new ArrayList<>();
    final Set<String> usedNames = new HashSet<>();
    int nextOffset = HEADER;
    public int paramsSize = 0;
    public int localsSize = 0;

    private final List<Boolean> tempInUse = new ArrayList<>();
    public int maxTemps = 0;

    public Frame(String label, int level, Frame parent) {
        this.label = label;
        this.level = level;
        this.parent = parent;
    }

    static int align4(int n) { return (Math.max(n, 1) + 3) / 4 * 4; }

    /** Reserva espacio en el marco y devuelve el offset. */
    int alloc(String name, String kind, int size) {
        int off = nextOffset;
        nextOffset += align4(size);
        slots.add(new Slot(name, kind, off, size));
        if (kind.equals("param")) paramsSize += align4(size); else localsSize += align4(size);
        return off;
    }

    // ---- temporales con reciclaje ----

    static boolean isTemp(String op) {
        if (op == null || op.length() < 2 || op.charAt(0) != 't') return false;
        for (int i = 1; i < op.length(); i++) if (!Character.isDigit(op.charAt(i))) return false;
        return true;
    }

    /** Asigna el temporal libre de menor índice (o crea uno nuevo). */
    public String newTemp() {
        for (int i = 0; i < tempInUse.size(); i++) {
            if (!tempInUse.get(i)) { tempInUse.set(i, true); return "t" + i; }
        }
        tempInUse.add(true);
        maxTemps = Math.max(maxTemps, tempInUse.size());
        return "t" + (tempInUse.size() - 1);
    }

    /** Devuelve un temporal al pool cuando su valor ya fue consumido. No hace nada si no es temporal. */
    public void free(String op) {
        if (!isTemp(op)) return;
        int i = Integer.parseInt(op.substring(1));
        if (i < tempInUse.size()) tempInUse.set(i, false);
    }

    public void freeAll() {
        for (int i = 0; i < tempInUse.size(); i++) tempInUse.set(i, false);
    }

    public int tempsOffset() { return nextOffset; }

    public int frameSize() { return nextOffset + maxTemps * 4; }

    public Map<String, Integer> slotMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Slot s : slots) m.put(s.name, s.offset);
        return m;
    }
}
