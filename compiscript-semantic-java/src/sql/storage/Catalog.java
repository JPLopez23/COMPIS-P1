package sql.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo de la base: tablas e índices declarados. Los nombres no distinguen mayúsculas. */
public class Catalog {

    public static class IndexDef {
        public final String name, table, column;
        public IndexDef(String name, String table, String column) { this.name = name; this.table = table; this.column = column; }
    }

    private final Map<String, TableSchema> tables = new LinkedHashMap<>();
    private final Map<String, IndexDef> indexes = new LinkedHashMap<>();

    public TableSchema table(String name) { return tables.get(name.toLowerCase()); }
    public IndexDef index(String name) { return indexes.get(name.toLowerCase()); }
    public List<TableSchema> tables() { return new ArrayList<>(tables.values()); }
    public List<IndexDef> indexes() { return new ArrayList<>(indexes.values()); }

    public void addTable(TableSchema t) { tables.put(t.name.toLowerCase(), t); }
    public void addIndex(IndexDef i) { indexes.put(i.name.toLowerCase(), i); }

    /** Quita la tabla y todos sus índices; devuelve los índices eliminados. */
    public List<IndexDef> removeTable(String name) {
        tables.remove(name.toLowerCase());
        List<IndexDef> removed = indexesOf(name);
        for (IndexDef d : removed) indexes.remove(d.name.toLowerCase());
        return removed;
    }

    public void removeIndex(String name) { indexes.remove(name.toLowerCase()); }

    /** Claves foráneas de otras tablas que apuntan a la tabla dada (las autorreferencias no cuentan). */
    public List<Map.Entry<TableSchema, TableSchema.ForeignKey>> referencesTo(String table) {
        List<Map.Entry<TableSchema, TableSchema.ForeignKey>> r = new ArrayList<>();
        for (TableSchema t : tables.values())
            for (TableSchema.ForeignKey fk : t.foreignKeys)
                if (fk.refTable.equalsIgnoreCase(table) && !t.name.equalsIgnoreCase(table))
                    r.add(new java.util.AbstractMap.SimpleEntry<>(t, fk));
        return r;
    }

    /** Todas las claves foráneas, autorreferencias incluidas, que apuntan a la tabla. */
    public List<Map.Entry<TableSchema, TableSchema.ForeignKey>> allReferencesTo(String table) {
        List<Map.Entry<TableSchema, TableSchema.ForeignKey>> r = new ArrayList<>();
        for (TableSchema t : tables.values())
            for (TableSchema.ForeignKey fk : t.foreignKeys)
                if (fk.refTable.equalsIgnoreCase(table)) r.add(new java.util.AbstractMap.SimpleEntry<>(t, fk));
        return r;
    }

    /** DROP TABLE ... CASCADE: quita las claves foráneas de otras tablas que apuntan a la tabla dada. */
    public void dropReferencesTo(String table) {
        for (TableSchema t : tables.values())
            if (!t.name.equalsIgnoreCase(table)) t.foreignKeys.removeIf(fk -> fk.refTable.equalsIgnoreCase(table));
    }

    public List<IndexDef> indexesOf(String table) {
        List<IndexDef> r = new ArrayList<>();
        for (IndexDef d : indexes.values()) if (d.table.equalsIgnoreCase(table)) r.add(d);
        return r;
    }

    /** Índice sobre la columna dada de la tabla, o null. */
    public IndexDef indexOn(String table, String column) {
        for (IndexDef d : indexes.values())
            if (d.table.equalsIgnoreCase(table) && d.column.equalsIgnoreCase(column)) return d;
        return null;
    }

    /** Copia del catálogo (sin datos), para que el análisis semántico simule los DDL de un script. */
    public Catalog copy() {
        Catalog c = new Catalog();
        for (TableSchema t : tables.values()) c.addTable(t.copy());
        for (IndexDef i : indexes.values()) c.addIndex(i);
        return c;
    }
}
