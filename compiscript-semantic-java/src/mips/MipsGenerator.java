package mips;

import codegen.Frame;
import codegen.TacGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generador de código MIPS: traduce el TAC (ya generado y con la información de los registros de activación)
 * a ensamblador MIPS32 para SPIM / MARS.
 *
 * <h3>Convención de llamada</h3>
 * <pre>
 *  Llamador                                   Llamado (prologue)
 *  --------                                   -------------------
 *  param x  → addiu $sp,$sp,-4 ; sw x,0($sp)  addiu $sp,$sp,-S           (S = tamaño del registro de activación)
 *  $a0 = enlace de acceso (marco definidor)   sw $ra,0($sp) ; sw $fp,4($sp) ; sw $a0,8($sp) ; move $fp,$sp
 *  jal f                                      copia los parámetros apilados a fp+16, fp+20…
 *  addiu $sp,$sp,4n   (desapila parámetros)   guarda los $s que usa
 *  resultado en $v0                           (epílogo) $v0 → fp+12, restaura $s, $ra, $fp y $sp ; jr $ra
 * </pre>
 * Registro de activación (desde $fp): 0 $ra · 4 enlace de control · 8 enlace de acceso · 12 valor de retorno ·
 * 16… parámetros, locales, temporales derramados y, al final, los $s guardados.
 */
public final class MipsGenerator {

    private final TacGenerator tac;
    private final Map<String, Integer> globals;
    private final Map<String, String> strings = new LinkedHashMap<>();

    /** Estadísticas del último {@link #generate()}. */
    public int totalSpills = 0;
    public int totalReloads = 0;
    public final Map<String, String> registerReport = new LinkedHashMap<>();

    public MipsGenerator(TacGenerator tac) {
        this.tac = tac;
        this.globals = tac.globalOffsets();
    }

    // ======================================================================
    // Punto de entrada
    // ======================================================================

    public String generate() {
        StringBuilder text = new StringBuilder();
        for (Frame f : tac.frames) {
            FunctionTranslator ft = new FunctionTranslator(f);
            text.append(ft.translate());
            totalSpills += ft.ra.spills;
            totalReloads += ft.ra.reloads;
            registerReport.put(f.label, "usa " + (ft.ra.usedSNames().isEmpty() ? "ningún $s" : String.join(", ", ft.ra.usedSNames()))
                    + " · derrames: " + ft.ra.spills + " · recargas: " + ft.ra.reloads);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# ======================================================================\n");
        sb.append("# Codigo MIPS generado por el compilador de Compiscript (Fase 3)\n");
        sb.append("# Se ejecuta en SPIM o MARS (el programa empieza en main)\n");
        sb.append("# ======================================================================\n");
        sb.append("        .data\n");
        sb.append(MipsRuntime.data(tac.globalSize));
        sb.append("        .align 2\n");
        for (Map.Entry<String, String> e : strings.entrySet())
            sb.append(e.getValue()).append(":  .asciiz \"").append(e.getKey()).append("\"\n");
        sb.append("\n        .text\n        .globl main\n\n");
        sb.append(text);
        sb.append(MipsRuntime.text());
        return asciiComments(sb.toString());
    }

    /** Los comentarios del ensamblador se dejan en ASCII (algunos simuladores leen el archivo como Latin-1). */
    static String asciiComments(String asm) {
        StringBuilder out = new StringBuilder();
        for (String line : asm.split("\n", -1)) {
            boolean inStr = false;
            int cut = -1;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') inStr = !inStr;
                else if (c == '#' && !inStr) { cut = i; break; }
            }
            if (cut >= 0) {
                String tail = java.text.Normalizer.normalize(line.substring(cut), java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").replaceAll("[^\\x00-\\x7F]", "?");
                line = line.substring(0, cut) + tail;
            }
            out.append(line).append("\n");
        }
        String r = out.toString();
        return r.endsWith("\n\n") ? r.substring(0, r.length() - 1) : r;
    }

    private String strLabel(String content) {
        return strings.computeIfAbsent(content, k -> "str_" + strings.size());
    }

    static String funcLabel(String tacLabel) {
        if (tacLabel.equals("main")) return "main";
        return "f_" + tacLabel.replace('$', 'S');
    }

    // ======================================================================
    // Traducción de una función
    // ======================================================================

    private final class FunctionTranslator {
        final Frame frame;
        final List<TacInstr> code;
        final List<String> body = new ArrayList<>();
        final RegisterAllocator ra;
        final Map<Frame, Map<String, Integer>> slotCache = new HashMap<>();
        int idx = 0;
        final boolean isMain;
        final String retLabel;

        FunctionTranslator(Frame frame) {
            this.frame = frame;
            this.code = TacParser.parse(frame.code);
            this.isMain = frame.label.equals("main");
            this.retLabel = "ret_" + funcLabel(frame.label);
            this.ra = new RegisterAllocator(code, frame.tempsOffset(), this::emit, () -> idx);
        }

        // ------------------------------------------------------------ emisión

        void emit(String s) { body.add("    " + s); }
        void emitRaw(String s) { body.add(s); }

        String translate() {
            for (idx = 0; idx < code.size(); idx++) translateInstr(code.get(idx));

            boolean hasHandlers = false;
            for (TacInstr in : code) if (in.kind == TacInstr.Kind.PUSHHANDLER) hasHandlers = true;
            int mask = hasHandlers ? 0xFF : ra.usedSMask();

            int params = 0;
            for (Frame.Slot s : frame.slots) if (s.kind.equals("param")) params++;
            int saved = Integer.bitCount(mask);
            int size = frame.frameSize() + 4 * saved;
            size = (size + 7) / 8 * 8;
            int savedBase = frame.frameSize();

            StringBuilder sb = new StringBuilder();
            sb.append("# ----------------------------------------------------------------------\n");
            sb.append("# función ").append(frame.label).append("  (nivel ").append(frame.level)
              .append(", registro de activación de ").append(size).append(" bytes)\n");
            sb.append(funcLabel(frame.label)).append(":\n");
            sb.append("    addiu $sp, $sp, -").append(size).append("      # reserva el registro de activación\n");
            sb.append("    sw $ra, 0($sp)           # direccion de retorno\n");
            sb.append("    sw $fp, 4($sp)           # enlace de control\n");
            sb.append("    sw $a0, 8($sp)           # enlace de acceso\n");
            sb.append("    move $fp, $sp\n");
            for (int i = 0; i < params; i++) {
                sb.append("    lw $t8, ").append(size + 4 * (params - 1 - i)).append("($fp)      # parametro ").append(i).append(" (apilado por el llamador)\n");
                sb.append("    sw $t8, ").append(Frame.HEADER + 4 * i).append("($fp)\n");
            }
            for (Frame.Slot s : frame.slots) {
                if (s.kind.equals("local") || s.kind.equals("oculta"))
                    sb.append("    sw $zero, ").append(s.offset).append("($fp)      # inicializa ").append(s.name).append(" en 0\n");
            }
            int k = 0;
            for (int i = 0; i < RegisterAllocator.S.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    sb.append("    sw ").append(RegisterAllocator.S[i]).append(", ").append(savedBase + 4 * k).append("($fp)      # guarda ").append(RegisterAllocator.S[i]).append("\n");
                    k++;
                }
            }
            // un `j ret` justo antes de la etiqueta de retorno es innecesario
            if (!body.isEmpty() && body.get(body.size() - 1).trim().equals("j " + retLabel)) body.remove(body.size() - 1);
            for (String l : body) sb.append(l).append('\n');

            sb.append(retLabel).append(":\n");
            if (isMain) {
                sb.append("    li $v0, 10               # fin del programa\n");
                sb.append("    syscall\n\n");
                return sb.toString();
            }
            k = 0;
            for (int i = 0; i < RegisterAllocator.S.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    sb.append("    lw ").append(RegisterAllocator.S[i]).append(", ").append(savedBase + 4 * k).append("($fp)      # restaura ").append(RegisterAllocator.S[i]).append("\n");
                    k++;
                }
            }
            sb.append("    lw $ra, 0($fp)\n");
            sb.append("    lw $t8, 4($fp)\n");
            sb.append("    addiu $sp, $fp, ").append(size).append("      # libera el registro de activación\n");
            sb.append("    move $fp, $t8\n");
            sb.append("    jr $ra\n\n");
            return sb.toString();
        }

        // ------------------------------------------------------------ operandos

        /** Direcciones de variables: desplazamiento en el marco que corresponde (siguiendo enlaces de acceso). */
        String varAddr(TacOperand o) {
            Frame f = frame;
            String base = "$fp";
            if (o.env > 0) {
                emit("move $t8, $fp");
                for (int k = 0; k < o.env; k++) {
                    emit("lw $t8, 8($t8)         # enlace de acceso");
                    f = f.parent;
                    if (f == null) throw new IllegalStateException("Enlace de acceso fuera de rango: " + o);
                }
                base = "$t8";
            }
            Map<String, Integer> slots = slotCache.computeIfAbsent(f, Frame::slotMap);
            Integer off = slots.get(o.name);
            if (off == null) throw new IllegalStateException("Variable sin ranura en el marco " + f.label + ": " + o.name);
            return off + "(" + base + ")";
        }

        String globalAddr(TacOperand o) {
            Integer off = globals.get(o.name);
            if (off == null) throw new IllegalStateException("Variable global desconocida: " + o.name);
            emit("la $t8, G_data");
            return off + "($t8)";
        }

        /** Registro con el valor del operando (un $s para los temporales, un $t de trabajo para el resto). */
        String rd(TacOperand o) {
            if (o.kind == TacOperand.Kind.TEMP) return ra.useTemp(o.tempName());
            if (o.kind == TacOperand.Kind.INT && o.value == 0) return "$zero";
            String r = ra.scratch();
            loadInto(r, o);
            return r;
        }

        void loadInto(String reg, TacOperand o) {
            switch (o.kind) {
                case INT:
                    if (o.value == 0) emit("move " + reg + ", $zero");
                    else emit("li " + reg + ", " + o.value);
                    break;
                case STR:
                    emit("la " + reg + ", " + strLabel(o.str));
                    break;
                case TEMP: {
                    String r = ra.useTemp(o.tempName());
                    if (!r.equals(reg)) emit("move " + reg + ", " + r);
                    break;
                }
                case VAR:
                    emit("lw " + reg + ", " + varAddr(o));
                    break;
                case GLOBAL:
                    emit("lw " + reg + ", " + globalAddr(o));
                    break;
                case FIELD: {
                    String b = rd(o.base);
                    emit("beqz " + b + ", __nullref");
                    emit("lw " + reg + ", " + o.offset + "(" + b + ")");
                    break;
                }
                case INDEX: {
                    indexAddress(o);
                    emit("lw " + reg + ", 4($t8)");
                    break;
                }
            }
        }

        /** Deja en $t8 la dirección de a[i] - 4 + 4 (es decir, base + 4*i) comprobando nulos y límites. */
        void indexAddress(TacOperand o) {
            String b = rd(o.base);
            String i = rd(o.index);
            emit("beqz " + b + ", __nullref");
            emit("lw $t8, 0(" + b + ")           # longitud");
            emit("bgeu " + i + ", $t8, __oob      # indice fuera de rango (incluye negativos)");
            emit("sll $t8, " + i + ", 2");
            emit("addu $t8, $t8, " + b);
        }

        void storeTo(TacOperand dest, String valueReg) {
            switch (dest.kind) {
                case VAR:
                    emit("sw " + valueReg + ", " + varAddr(dest));
                    break;
                case GLOBAL:
                    emit("sw " + valueReg + ", " + globalAddr(dest));
                    break;
                case FIELD: {
                    String b = rd(dest.base);
                    emit("beqz " + b + ", __nullref");
                    emit("sw " + valueReg + ", " + dest.offset + "(" + b + ")");
                    break;
                }
                case INDEX:
                    indexAddress(dest);
                    emit("sw " + valueReg + ", 4($t8)");
                    break;
                default:
                    throw new IllegalStateException("Destino inválido: " + dest);
            }
        }

        /** Registro donde se calculará el resultado de una instrucción. */
        String destReg(TacOperand d) {
            return d.isTemp() ? ra.defTemp(d.tempName()) : ra.scratch();
        }

        /** Si el destino no es un temporal, guarda el resultado donde corresponda. */
        void commit(TacOperand d, String reg) {
            if (!d.isTemp()) storeTo(d, reg);
        }

        /** Deja en reg el marco k niveles hacia afuera (siguiendo los enlaces de acceso). */
        void linkTo(String reg, int k) {
            emit("move " + reg + ", $fp");
            for (int i = 0; i < k; i++) emit("lw " + reg + ", 8(" + reg + ")      # enlace de acceso");
        }

        // ------------------------------------------------------------ instrucciones

        void translateInstr(TacInstr in) {
            if (in.kind != TacInstr.Kind.LABEL) emitRaw("    # " + in.source);
            switch (in.kind) {
                case LABEL:
                    ra.flushDirty();
                    ra.clear();
                    emitRaw(in.label + ":");
                    break;
                case GOTO:
                    ra.flushDirty();
                    emit("j " + in.label);
                    ra.clear();
                    break;
                case IF: {
                    String x = rd(in.a);
                    ra.flushDirty();
                    emit("bnez " + x + ", " + in.label);
                    break;
                }
                case IFFALSE: {
                    String x = rd(in.a);
                    ra.flushDirty();
                    emit("beqz " + x + ", " + in.label);
                    break;
                }
                case IFREL: translateIfRel(in); break;
                case COPY: translateCopy(in); break;
                case BIN: translateBin(in); break;
                case UNARY: {
                    String x = rd(in.a);
                    String d = destReg(in.dest);
                    if (in.op.equals("-")) emit("subu " + d + ", $zero, " + x);
                    else emit("seq " + d + ", " + x + ", $zero");
                    commit(in.dest, d);
                    break;
                }
                case LEN: {
                    String x = rd(in.a);
                    String d = destReg(in.dest);
                    emit("beqz " + x + ", __nullref");
                    emit("lw " + d + ", 0(" + x + ")");
                    commit(in.dest, d);
                    break;
                }
                case NEWARRAY: {
                    emit("li $a0, " + (4 + 4 * in.n));
                    emit("li $v0, 9              # sbrk");
                    emit("syscall");
                    emit("li $t8, " + in.n);
                    emit("sw $t8, 0($v0)         # longitud");
                    String d = destReg(in.dest);
                    emit("move " + d + ", $v0");
                    commit(in.dest, d);
                    break;
                }
                case NEW: {
                    emit("li $a0, " + Math.max(4, in.n));
                    emit("li $v0, 9              # sbrk");
                    emit("syscall");
                    String d = destReg(in.dest);
                    emit("move " + d + ", $v0");
                    commit(in.dest, d);
                    break;
                }
                case CLOSURE: {
                    emit("li $a0, 8");
                    emit("li $v0, 9              # sbrk");
                    emit("syscall");
                    emit("la $t8, " + funcLabel(in.label));
                    emit("sw $t8, 0($v0)         # codigo");
                    linkTo("$t9", in.link);
                    emit("sw $t9, 4($v0)         # entorno");
                    String d = destReg(in.dest);
                    emit("move " + d + ", $v0");
                    commit(in.dest, d);
                    break;
                }
                case EXCEPTION: {
                    String d = destReg(in.dest);
                    emit("lw " + d + ", __exc");
                    commit(in.dest, d);
                    break;
                }
                case PARAM: {
                    String x = rd(in.a);
                    emit("addiu $sp, $sp, -4");
                    emit("sw " + x + ", 0($sp)");
                    break;
                }
                case CALL: {
                    linkTo("$a0", in.link);
                    emit("jal " + funcLabel(in.label));
                    afterCall(in);
                    break;
                }
                case CALLIND: {
                    String c = rd(in.a);
                    emit("lw $t8, 0(" + c + ")           # codigo de la closure");
                    emit("lw $a0, 4(" + c + ")           # entorno de la closure");
                    emit("jalr $t8");
                    afterCall(in);
                    break;
                }
                case RETURN:
                    if (in.a != null) {
                        loadInto("$v0", in.a);
                        emit("sw $v0, 12($fp)        # valor de retorno en el registro de activación");
                    }
                    emit("j " + retLabel);
                    ra.clear();
                    break;
                case PRINT:
                    loadInto("$a0", in.a);
                    emit("jal __print_int");
                    break;
                case PRINTS:
                    loadInto("$a0", in.a);
                    emit("jal __print_str");
                    break;
                case PRINTB:
                    loadInto("$a0", in.a);
                    emit("jal __print_bool");
                    break;
                case PUSHHANDLER:
                    emit("la $t8, " + in.label);
                    emit("lw $t9, __hptr");
                    emit("sw $t8, 0($t9)");
                    emit("sw $sp, 4($t9)");
                    emit("sw $fp, 8($t9)");
                    emit("addiu $t9, $t9, 12");
                    emit("sw $t9, __hptr");
                    break;
                case POPHANDLER:
                    emit("lw $t9, __hptr");
                    emit("addiu $t9, $t9, -12");
                    emit("sw $t9, __hptr");
                    break;
            }
            ra.endInstruction();
        }

        void afterCall(TacInstr in) {
            if (in.n > 0) emit("addiu $sp, $sp, " + 4 * in.n + "      # desapila los parametros");
            if (in.dest != null) {
                String d = destReg(in.dest);
                emit("move " + d + ", $v0");
                commit(in.dest, d);
            }
        }

        void translateCopy(TacInstr in) {
            if (in.dest.isTemp()) {
                String d = ra.defTemp(in.dest.tempName());
                loadInto(d, in.a);
            } else {
                String v = rd(in.a);
                storeTo(in.dest, v);
            }
        }

        void translateIfRel(TacInstr in) {
            String op = in.op;
            if (op.equals("==s") || op.equals("!=s")) {
                loadInto("$a0", in.a);
                loadInto("$a1", in.b);
                ra.flushDirty();
                emit("jal __streq");
                emit((op.equals("==s") ? "bnez" : "beqz") + " $v0, " + in.label);
                return;
            }
            String x = rd(in.a);
            String y = rd(in.b);
            ra.flushDirty();
            String br;
            switch (op) {
                case "==": br = "beq"; break;
                case "!=": br = "bne"; break;
                case "<": br = "blt"; break;
                case "<=": br = "ble"; break;
                case ">": br = "bgt"; break;
                case ">=": br = "bge"; break;
                default: throw new IllegalStateException("Comparación desconocida: " + op);
            }
            emit(br + " " + x + ", " + y + ", " + in.label);
        }

        void translateBin(TacInstr in) {
            String op = in.op;
            if (op.equals("++") || op.equals("==s") || op.equals("!=s")) {
                loadInto("$a0", in.a);
                loadInto("$a1", in.b);
                emit("jal " + (op.equals("++") ? "__concat" : "__streq"));
                String d = destReg(in.dest);
                if (op.equals("!=s")) emit("xori " + d + ", $v0, 1");
                else emit("move " + d + ", $v0");
                commit(in.dest, d);
                return;
            }
            String x = rd(in.a);
            if ((op.equals("+") || op.equals("-")) && in.b.kind == TacOperand.Kind.INT) {
                int v = op.equals("+") ? in.b.value : -in.b.value;
                if (v >= -32768 && v <= 32767) {
                    String d = destReg(in.dest);
                    emit("addiu " + d + ", " + x + ", " + v);
                    commit(in.dest, d);
                    return;
                }
            }
            String y = rd(in.b);
            String d = destReg(in.dest);
            switch (op) {
                case "+": emit("addu " + d + ", " + x + ", " + y); break;
                case "-": emit("subu " + d + ", " + x + ", " + y); break;
                case "*": emit("mul " + d + ", " + x + ", " + y); break;
                case "/":
                    emit("beqz " + y + ", __div0");
                    emit("div " + x + ", " + y);
                    emit("mflo " + d);
                    break;
                case "%":
                    emit("beqz " + y + ", __div0");
                    emit("div " + x + ", " + y);
                    emit("mfhi " + d);
                    break;
                case "==": emit("seq " + d + ", " + x + ", " + y); break;
                case "!=": emit("sne " + d + ", " + x + ", " + y); break;
                case "<": emit("slt " + d + ", " + x + ", " + y); break;
                case "<=": emit("sle " + d + ", " + x + ", " + y); break;
                case ">": emit("sgt " + d + ", " + x + ", " + y); break;
                case ">=": emit("sge " + d + ", " + x + ", " + y); break;
                default: throw new IllegalStateException("Operador desconocido: " + op);
            }
            commit(in.dest, d);
        }
    }
}
