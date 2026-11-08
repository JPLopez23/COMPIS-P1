package mips;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulador de MIPS32 (subconjunto que usan SPIM y MARS) para ejecutar y validar el ensamblador generado.
 *
 * <p>Implementa los registros, memoria (datos, montón con sbrk y pila), las instrucciones y pseudo-instrucciones
 * que emite el generador y las llamadas al sistema básicas: 1 imprimir entero, 4 imprimir cadena, 5 leer entero,
 * 9 sbrk, 10 salir, 11 imprimir carácter, 17 salir con código.
 *
 * <p>Uso: {@code java -cp out mips.MipsSimulator programa.asm}
 */
public final class MipsSimulator {

    public static final class Result {
        public String output = "";
        public long steps;
        public boolean exited;      // terminó con la llamada al sistema de salida
        public int exitCode;
        public String error;        // error de ejecución del simulador (null si no hubo)
        public boolean ok() { return error == null && exited; }
    }

    private static final int TEXT_BASE = 0x00400000;
    private static final int DATA_BASE = 0x10010000;
    private static final int STACK_TOP = 0x7fffeffc;
    private static final int DATA_SIZE = 16 * 1024 * 1024;
    private static final int STACK_SIZE = 4 * 1024 * 1024;

    // ---------------------------------------------------------------- instrucciones

    private static final class Instr {
        String op;
        int rd, rs, rt;
        int imm;
        int addrBase = -1;      // registro base de la dirección (-1 = absoluta)
        long addrAbs;           // desplazamiento o dirección absoluta
        String label;
        int target = -1;
        String text;
        int line;
    }

    private static final Map<String, Integer> REGS = new HashMap<>();
    static {
        String[] names = {"zero", "at", "v0", "v1", "a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
                "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"};
        for (int i = 0; i < names.length; i++) REGS.put(names[i], i);
        REGS.put("s8", 30);
    }

    private final List<Instr> text = new ArrayList<>();
    private final Map<String, Long> labels = new HashMap<>();
    private byte[] data = new byte[1024];
    private int dataLen = 0;
    private final List<Object[]> relocs = new ArrayList<>(); // {offset, expresion}

    private final int[] reg = new int[32];
    private int hi, lo;
    private byte[] heap;
    private byte[] stack;
    private int brk;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private String stdin = "";
    private int stdinPos = 0;

    private MipsSimulator() {}

    // ---------------------------------------------------------------- API

    public static Result run(String asm) { return run(asm, 50_000_000L, ""); }

    public static Result run(String asm, long maxSteps, String stdin) {
        MipsSimulator sim = new MipsSimulator();
        Result res = new Result();
        try {
            sim.stdin = stdin == null ? "" : stdin;
            sim.assemble(asm);
            sim.execute(maxSteps, res);
        } catch (SimError e) {
            res.error = e.getMessage();
        } catch (RuntimeException e) {
            res.error = "error interno del simulador: " + e;
        }
        res.output = new String(sim.out.toByteArray(), StandardCharsets.UTF_8);
        return res;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: java -cp out mips.MipsSimulator programa.asm");
            System.exit(2);
        }
        String asm = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        Result r = run(asm);
        System.out.print(r.output);
        if (r.error != null) {
            System.err.println("Error de ejecución: " + r.error);
            System.exit(1);
        }
        System.exit(r.exitCode);
    }

    private static final class SimError extends RuntimeException {
        SimError(String m) { super(m); }
    }

    // ---------------------------------------------------------------- ensamblador

    private void assemble(String asm) {
        boolean inText = true;
        String[] lines = asm.split("\r?\n");
        for (int ln = 0; ln < lines.length; ln++) {
            String line = stripComment(lines[ln]).trim();
            while (true) {
                int colon = labelEnd(line);
                if (colon < 0) break;
                String name = line.substring(0, colon).trim();
                if (labels.containsKey(name)) throw new SimError("línea " + (ln + 1) + ": etiqueta repetida " + name);
                labels.put(name, inText ? (long) (TEXT_BASE + 4 * text.size()) : (long) (DATA_BASE + dataLen));
                line = line.substring(colon + 1).trim();
            }
            if (line.isEmpty()) continue;
            if (line.startsWith(".")) {
                String dir = line.split("\\s+")[0];
                String rest = line.substring(dir.length()).trim();
                switch (dir) {
                    case ".text": inText = true; break;
                    case ".data": inText = false; break;
                    case ".globl": case ".global": case ".ent": case ".end": break;
                    default:
                        if (inText) throw new SimError("línea " + (ln + 1) + ": directiva de datos en .text: " + dir);
                        dataDirective(ln + 1, dir, rest);
                }
                continue;
            }
            if (!inText) throw new SimError("línea " + (ln + 1) + ": instrucción en .data: " + line);
            Instr in = new Instr();
            in.text = line;
            in.line = ln + 1;
            int sp = indexOfSpace(line);
            in.op = (sp < 0 ? line : line.substring(0, sp)).toLowerCase();
            String rest = sp < 0 ? "" : line.substring(sp).trim();
            decode(in, splitArgs(rest));
            text.add(in);
        }
        // resolver etiquetas
        for (Instr in : text) {
            if (in.label != null) {
                Long a = labels.get(in.label);
                if (a == null) throw new SimError("línea " + in.line + ": etiqueta no definida " + in.label);
                if (isBranchOrJump(in.op)) in.target = (int) ((a - TEXT_BASE) / 4);
                else if (in.op.equals("la") || in.op.equals("li")) in.imm = (int) (long) a;
                else in.addrAbs += a;
            }
        }
        for (Object[] r : relocs) {
            int off = (Integer) r[0];
            long v = eval((String) r[1]);
            for (int k = 0; k < 4; k++) data[off + k] = (byte) (v >> (8 * k));
        }
        heap = new byte[DATA_SIZE];
        System.arraycopy(data, 0, heap, 0, dataLen);
        stack = new byte[STACK_SIZE];
        brk = (dataLen + 7) & ~7;
        reg[29] = STACK_TOP;
        reg[30] = STACK_TOP;
        Long main = labels.get("main");
        pc = main == null ? 0 : (int) ((main - TEXT_BASE) / 4);
    }

    private int pc;

    private static boolean isBranchOrJump(String op) {
        switch (op) {
            case "j": case "jal": case "b": case "beq": case "bne": case "beqz": case "bnez": case "blt": case "ble":
            case "bgt": case "bge": case "bltu": case "bleu": case "bgtu": case "bgeu": return true;
            default: return false;
        }
    }

    private static String stripComment(String s) {
        boolean in = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && in) { i++; continue; }
            if (c == '"') in = !in;
            else if (c == '#' && !in) return s.substring(0, i);
        }
        return s;
    }

    /** Posición del ':' de una etiqueta al inicio de la línea, o -1. */
    private static int labelEnd(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$') i++;
            else break;
        }
        if (i > 0 && i < s.length() && s.charAt(i) == ':') return i;
        return -1;
    }

    private static int indexOfSpace(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isWhitespace(s.charAt(i))) return i;
        return -1;
    }

    private static List<String> splitArgs(String s) {
        List<String> out = new ArrayList<>();
        if (s.isEmpty()) return out;
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (c == ',' && depth == 0) { out.add(cur.toString().trim()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString().trim());
        return out;
    }

    // ---------------------------------------------------------------- directivas de datos

    private void ensure(int extra) {
        if (dataLen + extra > data.length) {
            byte[] n = new byte[Math.max(data.length * 2, dataLen + extra)];
            System.arraycopy(data, 0, n, 0, dataLen);
            data = n;
        }
    }

    private void dataDirective(int ln, String dir, String rest) {
        switch (dir) {
            case ".align": {
                int n = (int) eval(rest);
                int a = 1 << n;
                int pad = (a - (dataLen % a)) % a;
                ensure(pad);
                dataLen += pad;
                break;
            }
            case ".space": {
                int n = (int) eval(rest);
                ensure(n);
                dataLen += n;
                break;
            }
            case ".word": {
                int pad = (4 - (dataLen % 4)) % 4;
                ensure(pad);
                dataLen += pad;
                for (String a : splitArgs(rest)) {
                    ensure(4);
                    if (a.matches("-?(0x[0-9a-fA-F]+|\\d+)")) {
                        long v = eval(a);
                        for (int k = 0; k < 4; k++) data[dataLen + k] = (byte) (v >> (8 * k));
                    } else {
                        relocs.add(new Object[]{dataLen, a});
                    }
                    dataLen += 4;
                }
                break;
            }
            case ".byte":
                for (String a : splitArgs(rest)) {
                    ensure(1);
                    data[dataLen++] = (byte) eval(a);
                }
                break;
            case ".asciiz":
            case ".ascii": {
                byte[] b = unquote(ln, rest);
                ensure(b.length + 1);
                System.arraycopy(b, 0, data, dataLen, b.length);
                dataLen += b.length;
                if (dir.equals(".asciiz")) data[dataLen++] = 0;
                break;
            }
            default:
                throw new SimError("línea " + ln + ": directiva no soportada " + dir);
        }
    }

    private byte[] unquote(int ln, String s) {
        s = s.trim();
        if (s.length() < 2 || s.charAt(0) != '"' || s.charAt(s.length() - 1) != '"')
            throw new SimError("línea " + ln + ": cadena mal formada " + s);
        s = s.substring(1, s.length() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '0': sb.append('\0'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append('\\').append(n);
                }
            } else sb.append(c);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Evalúa un entero, o «etiqueta», o «etiqueta+n». */
    private long eval(String e) {
        e = e.trim();
        if (e.matches("-?\\d+")) return Long.parseLong(e);
        if (e.matches("-?0x[0-9a-fA-F]+")) return Long.decode(e);
        int plus = Math.max(e.lastIndexOf('+'), -1);
        if (plus > 0) return eval(e.substring(0, plus)) + eval(e.substring(plus + 1));
        Long a = labels.get(e);
        if (a == null) throw new SimError("etiqueta no definida: " + e);
        return a;
    }

    // ---------------------------------------------------------------- decodificación

    private static boolean isReg(String s) { return s.startsWith("$"); }

    private static int regNum(String s) {
        String n = s.substring(1);
        if (n.matches("\\d+")) return Integer.parseInt(n);
        Integer r = REGS.get(n);
        if (r == null) throw new SimError("registro desconocido: " + s);
        return r;
    }

    private void decode(Instr in, List<String> a) {
        try {
            switch (in.op) {
                case "add": case "addu": case "sub": case "subu": case "mul": case "and": case "or": case "xor": case "nor":
                case "slt": case "sltu": case "seq": case "sne": case "sgt": case "sge": case "sle": case "rem":
                    in.rd = regNum(a.get(0)); in.rs = regNum(a.get(1)); in.rt = regNum(a.get(2));
                    break;
                case "addi": case "addiu": case "andi": case "ori": case "xori": case "slti": case "sltiu":
                case "sll": case "srl": case "sra":
                    in.rd = regNum(a.get(0)); in.rs = regNum(a.get(1)); in.imm = (int) eval(a.get(2));
                    break;
                case "li":
                    in.rd = regNum(a.get(0));
                    if (a.get(1).matches("-?(0x[0-9a-fA-F]+|\\d+)")) in.imm = (int) eval(a.get(1));
                    else in.label = a.get(1);
                    break;
                case "la":
                    in.rd = regNum(a.get(0));
                    in.label = a.get(1);
                    break;
                case "lui":
                    in.rd = regNum(a.get(0)); in.imm = (int) eval(a.get(1));
                    break;
                case "move":
                    in.rd = regNum(a.get(0)); in.rs = regNum(a.get(1));
                    break;
                case "lw": case "sw": case "lb": case "lbu": case "sb":
                    in.rt = regNum(a.get(0));
                    decodeAddr(in, a.get(1));
                    break;
                case "div": case "mult": case "multu": case "divu":
                    if (a.size() == 3) { in.rd = regNum(a.get(0)); in.rs = regNum(a.get(1)); in.rt = regNum(a.get(2)); in.imm = 1; }
                    else { in.rs = regNum(a.get(0)); in.rt = regNum(a.get(1)); }
                    break;
                case "mflo": case "mfhi":
                    in.rd = regNum(a.get(0));
                    break;
                case "beq": case "bne": case "blt": case "ble": case "bgt": case "bge":
                case "bltu": case "bleu": case "bgtu": case "bgeu":
                    in.rs = regNum(a.get(0));
                    if (isReg(a.get(1))) in.rt = regNum(a.get(1));
                    else { in.rt = -1; in.imm = (int) eval(a.get(1)); }
                    in.label = a.get(2);
                    break;
                case "beqz": case "bnez":
                    in.rs = regNum(a.get(0));
                    in.label = a.get(1);
                    break;
                case "j": case "jal": case "b":
                    in.label = a.get(0);
                    break;
                case "jr":
                    in.rs = regNum(a.get(0));
                    break;
                case "jalr":
                    if (a.size() == 2) { in.rd = regNum(a.get(0)); in.rs = regNum(a.get(1)); }
                    else { in.rd = 31; in.rs = regNum(a.get(0)); }
                    break;
                case "syscall": case "nop":
                    break;
                default:
                    throw new SimError("línea " + in.line + ": instrucción no soportada: " + in.op);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new SimError("línea " + in.line + ": operandos incorrectos en: " + in.text);
        }
    }

    private void decodeAddr(Instr in, String s) {
        s = s.trim();
        int lp = s.indexOf('(');
        if (lp >= 0) {
            String off = s.substring(0, lp).trim();
            String base = s.substring(lp + 1, s.indexOf(')')).trim();
            in.addrBase = regNum(base);
            if (off.isEmpty()) in.addrAbs = 0;
            else if (off.matches("-?(0x[0-9a-fA-F]+|\\d+)")) in.addrAbs = eval(off);
            else { in.label = off; }
        } else {
            in.addrBase = -1;
            if (s.matches("-?\\d+")) in.addrAbs = Long.parseLong(s);
            else {
                int plus = s.lastIndexOf('+');
                if (plus > 0 && s.substring(plus + 1).trim().matches("\\d+")) {
                    in.label = s.substring(0, plus).trim();
                    in.addrAbs = Long.parseLong(s.substring(plus + 1).trim());
                } else in.label = s;
            }
        }
    }

    // ---------------------------------------------------------------- memoria

    private byte[] seg(long addr, int len, String what, boolean[] isStack) {
        if (addr >= (DATA_BASE & 0xffffffffL) && addr + len <= (DATA_BASE & 0xffffffffL) + heap.length) {
            isStack[0] = false;
            return heap;
        }
        long stackLo = (STACK_TOP & 0xffffffffL) + 4 - STACK_SIZE;
        if (addr >= stackLo && addr + len <= (STACK_TOP & 0xffffffffL) + 4) {
            isStack[0] = true;
            return stack;
        }
        if (addr < stackLo && addr >= stackLo - (1 << 20))
            throw new SimError("desbordamiento de la pila (recursión demasiado profunda)");
        throw new SimError("dirección inválida al " + what + ": 0x" + Long.toHexString(addr));
    }

    private int index(long addr, boolean stackSeg) {
        if (stackSeg) return (int) (addr - ((STACK_TOP & 0xffffffffL) + 4 - STACK_SIZE));
        return (int) (addr - (DATA_BASE & 0xffffffffL));
    }

    private int loadWord(long addr) {
        if ((addr & 3) != 0) throw new SimError("acceso a palabra desalineado en 0x" + Long.toHexString(addr));
        boolean[] st = new boolean[1];
        byte[] m = seg(addr, 4, "leer", st);
        int i = index(addr, st[0]);
        return (m[i] & 0xff) | (m[i + 1] & 0xff) << 8 | (m[i + 2] & 0xff) << 16 | (m[i + 3] & 0xff) << 24;
    }

    private void storeWord(long addr, int v) {
        if ((addr & 3) != 0) throw new SimError("acceso a palabra desalineado en 0x" + Long.toHexString(addr));
        boolean[] st = new boolean[1];
        byte[] m = seg(addr, 4, "escribir", st);
        int i = index(addr, st[0]);
        m[i] = (byte) v;
        m[i + 1] = (byte) (v >> 8);
        m[i + 2] = (byte) (v >> 16);
        m[i + 3] = (byte) (v >> 24);
    }

    private int loadByte(long addr) {
        boolean[] st = new boolean[1];
        byte[] m = seg(addr, 1, "leer", st);
        return m[index(addr, st[0])];
    }

    private void storeByte(long addr, int v) {
        boolean[] st = new boolean[1];
        byte[] m = seg(addr, 1, "escribir", st);
        m[index(addr, st[0])] = (byte) v;
    }

    private long ea(Instr in) {
        long base = in.addrBase >= 0 ? (reg[in.addrBase] & 0xffffffffL) : 0;
        return (base + in.addrAbs) & 0xffffffffL;
    }

    // ---------------------------------------------------------------- ejecución

    private void setReg(int r, int v) {
        if (r != 0) reg[r] = v;
    }

    private void execute(long maxSteps, Result res) {
        long steps = 0;
        while (true) {
            if (pc < 0 || pc >= text.size()) throw new SimError("el contador de programa salió del código (pc=" + pc + ")");
            if (++steps > maxSteps) throw new SimError("se excedió el límite de " + maxSteps + " instrucciones (¿bucle infinito?)");
            Instr in = text.get(pc);
            int next = pc + 1;
            res.steps = steps;
            switch (in.op) {
                case "add": case "addu": setReg(in.rd, reg[in.rs] + reg[in.rt]); break;
                case "sub": case "subu": setReg(in.rd, reg[in.rs] - reg[in.rt]); break;
                case "mul": setReg(in.rd, reg[in.rs] * reg[in.rt]); break;
                case "and": setReg(in.rd, reg[in.rs] & reg[in.rt]); break;
                case "or": setReg(in.rd, reg[in.rs] | reg[in.rt]); break;
                case "xor": setReg(in.rd, reg[in.rs] ^ reg[in.rt]); break;
                case "nor": setReg(in.rd, ~(reg[in.rs] | reg[in.rt])); break;
                case "slt": setReg(in.rd, reg[in.rs] < reg[in.rt] ? 1 : 0); break;
                case "sltu": setReg(in.rd, Integer.compareUnsigned(reg[in.rs], reg[in.rt]) < 0 ? 1 : 0); break;
                case "seq": setReg(in.rd, reg[in.rs] == reg[in.rt] ? 1 : 0); break;
                case "sne": setReg(in.rd, reg[in.rs] != reg[in.rt] ? 1 : 0); break;
                case "sgt": setReg(in.rd, reg[in.rs] > reg[in.rt] ? 1 : 0); break;
                case "sge": setReg(in.rd, reg[in.rs] >= reg[in.rt] ? 1 : 0); break;
                case "sle": setReg(in.rd, reg[in.rs] <= reg[in.rt] ? 1 : 0); break;
                case "rem":
                    if (reg[in.rt] == 0) throw new SimError("división entre cero");
                    setReg(in.rd, reg[in.rs] % reg[in.rt]);
                    break;
                case "addi": case "addiu": setReg(in.rd, reg[in.rs] + in.imm); break;
                case "andi": setReg(in.rd, reg[in.rs] & in.imm); break;
                case "ori": setReg(in.rd, reg[in.rs] | in.imm); break;
                case "xori": setReg(in.rd, reg[in.rs] ^ in.imm); break;
                case "slti": setReg(in.rd, reg[in.rs] < in.imm ? 1 : 0); break;
                case "sltiu": setReg(in.rd, Integer.compareUnsigned(reg[in.rs], in.imm) < 0 ? 1 : 0); break;
                case "sll": setReg(in.rd, reg[in.rs] << in.imm); break;
                case "srl": setReg(in.rd, reg[in.rs] >>> in.imm); break;
                case "sra": setReg(in.rd, reg[in.rs] >> in.imm); break;
                case "li": case "la": setReg(in.rd, in.imm); break;
                case "lui": setReg(in.rd, in.imm << 16); break;
                case "move": setReg(in.rd, reg[in.rs]); break;
                case "lw": setReg(in.rt, loadWord(ea(in))); break;
                case "sw": storeWord(ea(in), reg[in.rt]); break;
                case "lb": setReg(in.rt, loadByte(ea(in))); break;
                case "lbu": setReg(in.rt, loadByte(ea(in)) & 0xff); break;
                case "sb": storeByte(ea(in), reg[in.rt]); break;
                case "div":
                    if (reg[in.rt] == 0) throw new SimError("división entre cero");
                    if (in.imm == 1) setReg(in.rd, reg[in.rs] / reg[in.rt]);
                    else { lo = reg[in.rs] / reg[in.rt]; hi = reg[in.rs] % reg[in.rt]; }
                    break;
                case "divu":
                    if (reg[in.rt] == 0) throw new SimError("división entre cero");
                    lo = Integer.divideUnsigned(reg[in.rs], reg[in.rt]);
                    hi = Integer.remainderUnsigned(reg[in.rs], reg[in.rt]);
                    break;
                case "mult": { long p = (long) reg[in.rs] * reg[in.rt]; lo = (int) p; hi = (int) (p >> 32); break; }
                case "multu": { long p = (reg[in.rs] & 0xffffffffL) * (reg[in.rt] & 0xffffffffL); lo = (int) p; hi = (int) (p >>> 32); break; }
                case "mflo": setReg(in.rd, lo); break;
                case "mfhi": setReg(in.rd, hi); break;
                case "beq": if (reg[in.rs] == cmpRt(in)) next = in.target; break;
                case "bne": if (reg[in.rs] != cmpRt(in)) next = in.target; break;
                case "blt": if (reg[in.rs] < cmpRt(in)) next = in.target; break;
                case "ble": if (reg[in.rs] <= cmpRt(in)) next = in.target; break;
                case "bgt": if (reg[in.rs] > cmpRt(in)) next = in.target; break;
                case "bge": if (reg[in.rs] >= cmpRt(in)) next = in.target; break;
                case "bltu": if (Integer.compareUnsigned(reg[in.rs], cmpRt(in)) < 0) next = in.target; break;
                case "bleu": if (Integer.compareUnsigned(reg[in.rs], cmpRt(in)) <= 0) next = in.target; break;
                case "bgtu": if (Integer.compareUnsigned(reg[in.rs], cmpRt(in)) > 0) next = in.target; break;
                case "bgeu": if (Integer.compareUnsigned(reg[in.rs], cmpRt(in)) >= 0) next = in.target; break;
                case "beqz": if (reg[in.rs] == 0) next = in.target; break;
                case "bnez": if (reg[in.rs] != 0) next = in.target; break;
                case "j": case "b": next = in.target; break;
                case "jal": setReg(31, TEXT_BASE + 4 * (pc + 1)); next = in.target; break;
                case "jr": next = codeIndex(reg[in.rs]); break;
                case "jalr": { int t = codeIndex(reg[in.rs]); setReg(in.rd, TEXT_BASE + 4 * (pc + 1)); next = t; break; }
                case "nop": break;
                case "syscall":
                    if (syscall(res)) return;
                    break;
                default:
                    throw new SimError("instrucción no soportada: " + in.op);
            }
            pc = next;
        }
    }

    private int cmpRt(Instr in) { return in.rt >= 0 ? reg[in.rt] : in.imm; }

    private int codeIndex(int addr) {
        if (addr < TEXT_BASE || ((addr - TEXT_BASE) & 3) != 0) throw new SimError("salto a una dirección de código inválida: 0x" + Integer.toHexString(addr));
        return (addr - TEXT_BASE) / 4;
    }

    /** Devuelve true si el programa terminó. */
    private boolean syscall(Result res) {
        int v0 = reg[2];
        switch (v0) {
            case 1:
                print(Integer.toString(reg[4]));
                return false;
            case 4: {
                long a = reg[4] & 0xffffffffL;
                while (true) {
                    int b = loadByte(a++);
                    if (b == 0) break;
                    out.write(b);
                }
                return false;
            }
            case 5: {
                int i = stdinPos;
                while (i < stdin.length() && Character.isWhitespace(stdin.charAt(i))) i++;
                int j = i;
                while (j < stdin.length() && !Character.isWhitespace(stdin.charAt(j))) j++;
                String tok = stdin.substring(i, j);
                stdinPos = j;
                try { reg[2] = tok.isEmpty() ? 0 : Integer.parseInt(tok); } catch (NumberFormatException e) { reg[2] = 0; }
                return false;
            }
            case 9: {
                int n = reg[4];
                if (n < 0) throw new SimError("sbrk con tamaño negativo");
                int addr = DATA_BASE + brk;
                brk += (n + 3) & ~3;
                if (brk > heap.length) throw new SimError("se agotó la memoria dinámica");
                reg[2] = addr;
                return false;
            }
            case 10:
                res.exited = true;
                res.exitCode = 0;
                return true;
            case 11:
                out.write(reg[4] & 0xff);
                return false;
            case 17:
                res.exited = true;
                res.exitCode = reg[4];
                return true;
            default:
                throw new SimError("llamada al sistema no soportada: " + v0);
        }
    }

    private void print(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(b, 0, b.length);
    }
}
