package mips;

import java.util.List;

/** Operando de una instrucción TAC, ya interpretado (ver docs/TAC.md, sección «Operandos»). */
final class TacOperand {

    enum Kind {
        INT,     // constante entera, true/false/null
        STR,     // literal de cadena
        TEMP,    // temporal tN
        VAR,     // variable local o parámetro (de este marco o de uno exterior: env(d).x)
        GLOBAL,  // G.x
        FIELD,   // base.campo@desplazamiento
        INDEX    // base[índice]
    }

    final Kind kind;
    final String text;

    int value;            // INT
    String str;           // STR (contenido sin comillas)
    int tempIndex;        // TEMP
    String name;          // VAR, GLOBAL y nombre del campo en FIELD
    int env;              // VAR: niveles de enlace de acceso a seguir
    int offset;           // FIELD
    TacOperand base;      // FIELD, INDEX
    TacOperand index;     // INDEX

    private TacOperand(Kind kind, String text) {
        this.kind = kind;
        this.text = text;
    }

    boolean isTemp() { return kind == Kind.TEMP; }
    String tempName() { return "t" + tempIndex; }

    /** Temporales que se leen al evaluar este operando (la base y el índice de un acceso también se leen). */
    void collectTemps(List<String> out) {
        if (kind == Kind.TEMP) out.add(tempName());
        if (base != null) base.collectTemps(out);
        if (index != null) index.collectTemps(out);
    }

    static TacOperand parse(String t) {
        if (t.startsWith("\"")) {
            TacOperand o = new TacOperand(Kind.STR, t);
            o.str = t.length() >= 2 ? t.substring(1, t.length() - 1) : "";
            return o;
        }
        if (t.equals("true")) return intConst(t, 1);
        if (t.equals("false") || t.equals("null")) return intConst(t, 0);
        if (t.matches("-?\\d+")) return intConst(t, (int) Long.parseLong(t));

        if (t.endsWith("]") && t.indexOf('[') > 0) {
            int lb = t.lastIndexOf('[');
            TacOperand o = new TacOperand(Kind.INDEX, t);
            o.base = parse(t.substring(0, lb));
            o.index = parse(t.substring(lb + 1, t.length() - 1));
            return o;
        }
        int at = t.lastIndexOf('@');
        if (at > 0) {
            String pre = t.substring(0, at);
            int dot = pre.lastIndexOf('.');
            TacOperand o = new TacOperand(Kind.FIELD, t);
            o.base = parse(pre.substring(0, dot));
            o.name = pre.substring(dot + 1);
            o.offset = Integer.parseInt(t.substring(at + 1));
            return o;
        }
        if (t.startsWith("G.")) {
            TacOperand o = new TacOperand(Kind.GLOBAL, t);
            o.name = t.substring(2);
            return o;
        }
        if (t.startsWith("env(")) {
            int close = t.indexOf(')');
            TacOperand o = new TacOperand(Kind.VAR, t);
            o.env = Integer.parseInt(t.substring(4, close));
            o.name = t.substring(close + 2);
            return o;
        }
        if (t.matches("t\\d+")) {
            TacOperand o = new TacOperand(Kind.TEMP, t);
            o.tempIndex = Integer.parseInt(t.substring(1));
            return o;
        }
        TacOperand o = new TacOperand(Kind.VAR, t);
        o.name = t;
        return o;
    }

    private static TacOperand intConst(String text, int v) {
        TacOperand o = new TacOperand(Kind.INT, text);
        o.value = v;
        return o;
    }

    @Override public String toString() { return text; }
}
