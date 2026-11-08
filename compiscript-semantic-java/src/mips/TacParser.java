package mips;

import java.util.ArrayList;
import java.util.List;

/** Convierte las líneas de texto del TAC de un marco en instrucciones estructuradas. */
final class TacParser {
    private TacParser() {}

    static List<TacInstr> parse(List<String> lines) {
        List<TacInstr> out = new ArrayList<>();
        for (String raw : lines) {
            String line = stripComment(raw);
            if (line.trim().isEmpty()) continue;
            out.add(parseLine(raw.trim(), line));
        }
        return out;
    }

    // ---------------------------------------------------------------- utilidades

    /** Quita el comentario («; ...») respetando las comillas de las cadenas. */
    static String stripComment(String s) {
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inStr = !inStr;
            else if (c == ';' && !inStr) return s.substring(0, i);
        }
        return s;
    }

    /** Separa por espacios, dejando intactas las cadenas entre comillas. */
    static List<String> tokenize(String s) {
        List<String> tk = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inStr = !inStr;
            if (Character.isWhitespace(c) && !inStr) {
                if (cur.length() > 0) { tk.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) tk.add(cur.toString());
        return tk;
    }

    private static String noComma(String t) {
        return t.endsWith(",") ? t.substring(0, t.length() - 1) : t;
    }

    private static int envDepth(String t) {
        // env(k)
        return Integer.parseInt(t.substring(4, t.indexOf(')')));
    }

    private static boolean isRef(TacOperand o) {
        return o.kind == TacOperand.Kind.FIELD || o.kind == TacOperand.Kind.INDEX;
    }

    // ---------------------------------------------------------------- análisis de una línea

    private static TacInstr parseLine(String source, String line) {
        if (!line.startsWith(" ") && line.trim().endsWith(":")) {
            TacInstr i = new TacInstr(TacInstr.Kind.LABEL, source);
            String t = line.trim();
            i.label = t.substring(0, t.length() - 1);
            return i;
        }
        List<String> tk = tokenize(line);
        String first = tk.get(0);

        switch (first) {
            case "goto": {
                TacInstr i = new TacInstr(TacInstr.Kind.GOTO, source);
                i.label = tk.get(1);
                return i;
            }
            case "if": {
                if (tk.size() == 4) { // if x goto L
                    TacInstr i = new TacInstr(TacInstr.Kind.IF, source);
                    i.a = TacOperand.parse(tk.get(1));
                    i.label = tk.get(3);
                    return i;
                }
                TacInstr i = new TacInstr(TacInstr.Kind.IFREL, source); // if a op b goto L
                i.a = TacOperand.parse(tk.get(1));
                i.op = tk.get(2);
                i.b = TacOperand.parse(tk.get(3));
                i.label = tk.get(5);
                return i;
            }
            case "ifFalse": {
                TacInstr i = new TacInstr(TacInstr.Kind.IFFALSE, source);
                i.a = TacOperand.parse(tk.get(1));
                i.label = tk.get(3);
                return i;
            }
            case "param": return unary(TacInstr.Kind.PARAM, source, tk);
            case "print": return unary(TacInstr.Kind.PRINT, source, tk);
            case "prints": return unary(TacInstr.Kind.PRINTS, source, tk);
            case "printb": return unary(TacInstr.Kind.PRINTB, source, tk);
            case "return": {
                TacInstr i = new TacInstr(TacInstr.Kind.RETURN, source);
                if (tk.size() > 1) i.a = TacOperand.parse(tk.get(1));
                return i;
            }
            case "pushhandler": {
                TacInstr i = new TacInstr(TacInstr.Kind.PUSHHANDLER, source);
                i.label = tk.get(1);
                return i;
            }
            case "pophandler": return new TacInstr(TacInstr.Kind.POPHANDLER, source);
            case "call": return parseCall(null, source, tk, 0);
            case "callind": return parseCall(null, source, tk, 0);
            default:
                break;
        }

        if (tk.size() >= 3 && tk.get(1).equals("=")) return parseAssign(source, tk);
        throw new IllegalArgumentException("Instrucción TAC no reconocida: " + source);
    }

    private static TacInstr unary(TacInstr.Kind k, String source, List<String> tk) {
        TacInstr i = new TacInstr(k, source);
        i.a = TacOperand.parse(tk.get(1));
        return i;
    }

    /** call f, n, env(k)  |  callind c, n  (start = índice del primer token «call»). */
    private static TacInstr parseCall(TacOperand dest, String source, List<String> tk, int start) {
        boolean ind = tk.get(start).equals("callind");
        TacInstr i = new TacInstr(ind ? TacInstr.Kind.CALLIND : TacInstr.Kind.CALL, source);
        i.dest = dest;
        String target = noComma(tk.get(start + 1));
        i.n = Integer.parseInt(noComma(tk.get(start + 2)));
        if (ind) i.a = TacOperand.parse(target);
        else {
            i.label = target;
            i.link = envDepth(tk.get(start + 3));
        }
        return i;
    }

    private static TacInstr parseAssign(String source, List<String> tk) {
        TacOperand dest = TacOperand.parse(tk.get(0));
        String r0 = tk.get(2);
        int n = tk.size();

        if (r0.equals("call") || r0.equals("callind")) return parseCall(dest, source, tk, 2);

        if (r0.equals("newarray")) {
            TacInstr i = new TacInstr(TacInstr.Kind.NEWARRAY, source);
            i.dest = dest;
            i.n = Integer.parseInt(tk.get(3));
            return i;
        }
        if (r0.equals("new")) {
            TacInstr i = new TacInstr(TacInstr.Kind.NEW, source);
            i.dest = dest;
            i.className = noComma(tk.get(3));
            i.n = Integer.parseInt(tk.get(4));
            return i;
        }
        if (r0.equals("closure")) {
            TacInstr i = new TacInstr(TacInstr.Kind.CLOSURE, source);
            i.dest = dest;
            i.label = noComma(tk.get(3));
            i.link = envDepth(tk.get(4));
            return i;
        }
        if (r0.equals("exception") && n == 3) {
            TacInstr i = new TacInstr(TacInstr.Kind.EXCEPTION, source);
            i.dest = dest;
            return i;
        }
        if (r0.equals("len") && n == 4) {
            TacInstr i = new TacInstr(TacInstr.Kind.LEN, source);
            i.dest = dest;
            i.a = TacOperand.parse(tk.get(3));
            return i;
        }
        if ((r0.equals("-") || r0.equals("!")) && n == 4) {
            TacInstr i = new TacInstr(TacInstr.Kind.UNARY, source);
            i.dest = dest;
            i.op = r0;
            i.a = TacOperand.parse(tk.get(3));
            return i;
        }
        if (n == 5) { // d = a op b
            TacInstr i = new TacInstr(TacInstr.Kind.BIN, source);
            i.dest = dest;
            i.a = TacOperand.parse(tk.get(2));
            i.op = tk.get(3);
            i.b = TacOperand.parse(tk.get(4));
            return i;
        }
        if (n == 3) { // d = s   (la lectura o la escritura indirecta la resuelve el generador)
            TacInstr i = new TacInstr(TacInstr.Kind.COPY, source);
            i.dest = dest;
            i.a = TacOperand.parse(r0);
            return i;
        }
        throw new IllegalArgumentException("Asignación TAC no reconocida: " + source);
    }
}
