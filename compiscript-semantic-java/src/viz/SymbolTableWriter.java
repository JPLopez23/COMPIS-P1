package viz;

import semantic.Scope;
import semantic.Symbol;

import java.util.List;

/** Renderiza la tabla de símbolos a Markdown y a consola. */
public class SymbolTableWriter {

    public String toMarkdown(List<Scope> allScopes) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tabla de símbolos\n\n");
        for (Scope sc : allScopes) {
            sb.append("## ").append(sc.fullLabel()).append("\n");
            String parent = sc.parent != null ? "#" + sc.parent.id : "—";
            sb.append("- padre: `").append(parent).append("`\n");
            if (sc.symbols.isEmpty()) {
                sb.append("- (vacío)\n\n");
                continue;
            }
            sb.append("\n");
            sb.append("| nombre | clase | tipo | línea | col |\n");
            sb.append("|---|---|---|---|---|\n");
            for (Symbol s : sc.symbols.values()) {
                sb.append("| `").append(s.name).append("` | ").append(s.kind).append(" | `")
                  .append(s.type).append("` | ").append(s.line).append(" | ").append(s.column).append(" |\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void printConsole(List<Scope> allScopes) {
        for (Scope sc : allScopes) {
            System.out.println("══════ " + sc.fullLabel() + " ══════");
            if (sc.symbols.isEmpty()) {
                System.out.println("  (vacío)");
                continue;
            }
            for (Symbol s : sc.symbols.values()) {
                System.out.printf("  %-18s %-10s %-20s L%d:C%d%n",
                    s.name, s.kind, s.type.toString(), s.line, s.column);
            }
        }
    }
}
