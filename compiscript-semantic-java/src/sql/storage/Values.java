package sql.storage;

import sql.SqlType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Valores en tiempo de ejecución: Long (INT), Double (FLOAT), String (VARCHAR), Boolean (BOOLEAN),
 * LocalDate (DATE) y null (NULL). Aquí viven la comparación, la conversión y el formato de texto.
 */
public final class Values {
    private Values() {}

    public static SqlType typeOf(Object v) {
        if (v == null) return SqlType.NULL;
        if (v instanceof Long) return SqlType.INT;
        if (v instanceof Double) return SqlType.FLOAT;
        if (v instanceof String) return SqlType.VARCHAR;
        if (v instanceof Boolean) return SqlType.BOOLEAN;
        if (v instanceof LocalDate) return SqlType.DATE;
        return SqlType.UNKNOWN;
    }

    /** Compara dos valores no nulos del mismo tipo (INT y FLOAT se mezclan como números). */
    public static int compare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            if (a instanceof Long && b instanceof Long) return Long.compare((Long) a, (Long) b);
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (a instanceof LocalDate && b instanceof String) b = toDate((String) b);
        if (a instanceof String && b instanceof LocalDate) a = toDate((String) a);
        if (a instanceof String && b instanceof String) return ((String) a).compareTo((String) b);
        if (a instanceof LocalDate && b instanceof LocalDate) return ((LocalDate) a).compareTo((LocalDate) b);
        if (a instanceof Boolean && b instanceof Boolean) return Boolean.compare((Boolean) a, (Boolean) b);
        throw new IllegalArgumentException("No se pueden comparar " + typeOf(a) + " y " + typeOf(b));
    }

    /** Igualdad entre valores no nulos. */
    public static boolean equal(Object a, Object b) {
        return compare(a, b) == 0;
    }

    public static boolean isValidDate(String s) {
        return toDate(s) != null;
    }

    /** Fecha YYYY-MM-DD estricta, o null si el texto no lo es. */
    public static LocalDate toDate(String s) {
        if (s == null || !s.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Convierte un valor al tipo de una columna (para almacenar o buscar en índices).
     * Devuelve null si el valor no se puede representar sin perder información.
     */
    public static Object coerce(Object v, SqlType target) {
        if (v == null) return null;
        switch (target) {
            case INT:
                if (v instanceof Long) return v;
                if (v instanceof Double && (Double) v == Math.rint((Double) v)) return (long) (double) (Double) v;
                return null;
            case FLOAT:
                return v instanceof Number ? (Object) ((Number) v).doubleValue() : null;
            case VARCHAR:
                return v instanceof String ? v : null;
            case BOOLEAN:
                return v instanceof Boolean ? v : null;
            case DATE:
                if (v instanceof LocalDate) return v;
                return v instanceof String ? toDate((String) v) : null;
            default:
                return null;
        }
    }

    /** Texto legible para mostrar un valor en resultados. */
    public static String display(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.format("%.1f", d);
            return Double.toString(d);
        }
        return v.toString();
    }

    // ── Texto de archivos (CSV e índices) ───────────────────────────────────

    /** Codifica un campo CSV: vacío para NULL, comillas solo cuando hacen falta. */
    public static String toCsvField(Object v) {
        if (v == null) return "";
        if (!(v instanceof String)) return v.toString();
        String s = (String) v;
        boolean needsQuotes = s.isEmpty() || s.contains(",") || s.contains("\"") || s.contains("\\")
            || s.contains("\n") || s.contains("\r") || !s.equals(s.trim());
        if (!needsQuotes) return s;
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    /** Interpreta un campo CSV según el tipo de la columna; quoted indica si venía entre comillas. */
    public static Object fromCsvField(String text, boolean quoted, SqlType type) {
        if (!quoted && text.isEmpty()) return null;
        switch (type) {
            case INT:     return Long.parseLong(text.trim());
            case FLOAT:   return Double.parseDouble(text.trim());
            case BOOLEAN: return Boolean.parseBoolean(text.trim());
            case DATE:    return LocalDate.parse(text.trim());
            default:      return text;
        }
    }

    /** Divide una línea CSV en campos; el resultado alterna texto y bandera de comillas. */
    public static java.util.List<Object[]> splitCsv(String line) {
        java.util.List<Object[]> fields = new java.util.ArrayList<>();
        int i = 0, n = line.length();
        while (true) {
            StringBuilder sb = new StringBuilder();
            boolean quoted = false;
            if (i < n && line.charAt(i) == '"') {
                quoted = true;
                i++;
                while (i < n) {
                    char c = line.charAt(i);
                    if (c == '"') {
                        if (i + 1 < n && line.charAt(i + 1) == '"') { sb.append('"'); i += 2; continue; }
                        i++;
                        break;
                    }
                    if (c == '\\' && i + 1 < n) {
                        char d = line.charAt(i + 1);
                        sb.append(d == 'n' ? '\n' : d == 'r' ? '\r' : d);
                        i += 2;
                        continue;
                    }
                    sb.append(c);
                    i++;
                }
            } else {
                while (i < n && line.charAt(i) != ',') sb.append(line.charAt(i++));
            }
            fields.add(new Object[]{sb.toString(), quoted});
            if (i < n && line.charAt(i) == ',') { i++; continue; }
            break;
        }
        return fields;
    }
}
