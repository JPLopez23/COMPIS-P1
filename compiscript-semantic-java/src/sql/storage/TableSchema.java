package sql.storage;

import sql.SqlType;

import java.util.ArrayList;
import java.util.List;

/** Esquema de una tabla: nombre y columnas con tipo y restricciones. */
public class TableSchema {

    public static class Column {
        public final String name; public final SqlType type; public final int size;
        public final boolean primaryKey, notNull;
        public Column(String name, SqlType type, int size, boolean primaryKey, boolean notNull) {
            this.name = name; this.type = type; this.size = size;
            this.primaryKey = primaryKey; this.notNull = notNull || primaryKey;
        }
        /** Tipo como se escribe en SQL, por ejemplo VARCHAR(50). */
        public String typeText() { return type == SqlType.VARCHAR ? "VARCHAR(" + size + ")" : type.name(); }
    }

    public final String name;
    public final List<Column> columns = new ArrayList<>();

    public TableSchema(String name) { this.name = name; }

    /** Posición de la columna (sin distinguir mayúsculas), o -1. */
    public int indexOf(String column) {
        for (int i = 0; i < columns.size(); i++) if (columns.get(i).name.equalsIgnoreCase(column)) return i;
        return -1;
    }

    public TableSchema copy() {
        TableSchema t = new TableSchema(name);
        t.columns.addAll(columns);
        return t;
    }
}
