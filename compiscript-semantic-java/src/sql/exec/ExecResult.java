package sql.exec;

import sql.storage.Values;

import java.util.ArrayList;
import java.util.List;

/** Resultado de ejecutar una sentencia: una tabla de filas (SELECT) o un mensaje (DDL y DML). */
public class ExecResult {
    public final String statement;
    public final List<String> columns = new ArrayList<>();
    public final List<Object[]> rows = new ArrayList<>();
    public String message = "";
    public boolean isQuery;
    /** Cómo se resolvió la consulta, por ejemplo qué índices se usaron. */
    public final List<String> notes = new ArrayList<>();
    /** Filas leídas de los archivos .csv para resolver la sentencia. */
    public long rowsRead;

    public ExecResult(String statement) { this.statement = statement; }

    /** Tabla en texto con bordes, para consola. */
    public String format() {
        StringBuilder sb = new StringBuilder();
        if (isQuery) {
            int n = columns.size();
            int[] w = new int[n];
            for (int i = 0; i < n; i++) w[i] = columns.get(i).length();
            for (Object[] r : rows) for (int i = 0; i < n; i++) w[i] = Math.max(w[i], Values.display(r[i]).length());
            line(sb, w);
            row(sb, w, columns.toArray());
            line(sb, w);
            for (Object[] r : rows) {
                String[] cells = new String[n];
                for (int i = 0; i < n; i++) cells[i] = Values.display(r[i]);
                row(sb, w, cells);
            }
            line(sb, w);
            sb.append(rows.size()).append(rows.size() == 1 ? " fila" : " filas").append('\n');
        } else {
            sb.append(message).append('\n');
        }
        for (String note : notes) sb.append("  · ").append(note).append('\n');
        return sb.toString();
    }

    private static void line(StringBuilder sb, int[] w) {
        sb.append('+');
        for (int x : w) { for (int i = 0; i < x + 2; i++) sb.append('-'); sb.append('+'); }
        sb.append('\n');
    }

    private static void row(StringBuilder sb, int[] w, Object[] cells) {
        sb.append('|');
        for (int i = 0; i < w.length; i++) {
            String c = String.valueOf(cells[i]);
            sb.append(' ').append(c);
            for (int k = c.length(); k < w[i]; k++) sb.append(' ');
            sb.append(" |");
        }
        sb.append('\n');
    }
}
