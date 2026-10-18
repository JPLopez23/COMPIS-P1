package codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Optimizaciones locales sobre el TAC de un marco: saltos redundantes y etiquetas sin uso. */
final class Peephole {
    private Peephole() {}

    private static final Pattern REL = Pattern.compile("^    if (.+) (==|!=|<=|>=|<|>) (.+) goto (L\\d+)$");
    private static final Pattern TRUTH = Pattern.compile("^    if (.+) goto (L\\d+)$");
    private static final Pattern LABEL_REF = Pattern.compile("\\bL\\d+\\b");

    static List<String> optimize(List<String> in) {
        List<String> code = new ArrayList<>(in);
        boolean changed = true;
        while (changed) {
            changed = false;

            // (1) goto L seguido (directamente o tras otras etiquetas) de L:  ->  se elimina el goto
            for (int i = 0; i < code.size(); i++) {
                String t = code.get(i).trim();
                if (!t.startsWith("goto ")) continue;
                String target = t.substring(5);
                for (int j = i + 1; j < code.size() && isLabel(code.get(j)); j++) {
                    if (labelName(code.get(j)).equals(target)) {
                        code.remove(i);
                        changed = true;
                        i--;
                        break;
                    }
                }
            }

            // (2) if c goto A ; goto B ; A:   ->   if !c goto B ; A:   (relación invertida)
            for (int i = 0; i + 1 < code.size(); i++) {
                String nxt = code.get(i + 1).trim();
                if (!nxt.startsWith("goto ")) continue;
                String b = nxt.substring(5);
                String a;
                String repl;
                Matcher m = REL.matcher(code.get(i));
                Matcher tr = TRUTH.matcher(code.get(i));
                if (m.matches()) {
                    a = m.group(4);
                    repl = "    if " + m.group(1) + " " + invert(m.group(2)) + " " + m.group(3) + " goto " + b;
                } else if (tr.matches()) {
                    a = tr.group(2);
                    repl = "    ifFalse " + tr.group(1) + " goto " + b;
                } else continue;
                boolean followed = false;
                for (int j = i + 2; j < code.size() && isLabel(code.get(j)); j++) {
                    if (labelName(code.get(j)).equals(a)) { followed = true; break; }
                }
                if (followed) {
                    code.set(i, repl);
                    code.remove(i + 1);
                    changed = true;
                }
            }

            // (3) etiquetas a las que nadie salta
            Set<String> refs = new HashSet<>();
            for (String l : code) {
                if (isLabel(l)) continue;
                Matcher lm = LABEL_REF.matcher(l);
                while (lm.find()) refs.add(lm.group());
            }
            for (int i = 0; i < code.size(); i++) {
                if (isLabel(code.get(i)) && !refs.contains(labelName(code.get(i)))) {
                    code.remove(i);
                    changed = true;
                    i--;
                }
            }
        }
        return code;
    }

    private static boolean isLabel(String l) { return !l.startsWith(" ") && l.endsWith(":"); }
    private static String labelName(String l) { return l.substring(0, l.length() - 1); }

    private static String invert(String op) {
        switch (op) {
            case "==": return "!=";
            case "!=": return "==";
            case "<": return ">=";
            case "<=": return ">";
            case ">": return "<=";
            default: return "<";
        }
    }
}
