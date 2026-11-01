package sql.storage;

import sql.SqlType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de datos en disco. Solo usa archivos y carpetas:
 *   metadata.txt         esquemas (tablas, columnas, tipos, restricciones) e índices declarados
 *   NOMBRE.csv           una tabla: cabecera con los nombres de columna y una fila por línea
 *   indexes/NOMBRE.idx   un índice: clave y posiciones (offsets) de las filas en el .csv
 */
public class Database {

    /** Fila leída del .csv junto con su posición en bytes. */
    public static class StoredRow {
        public final long offset; public final Object[] values;
        public StoredRow(long offset, Object[] values) { this.offset = offset; this.values = values; }
    }

    private final Path dir;
    private final Catalog catalog = new Catalog();
    private final Map<String, IndexData> indexCache = new HashMap<>();
    /** Filas leídas de los .csv desde el último resetStats; permite comprobar que un índice evita recorrer todo. */
    public long rowsRead;

    public Database(Path dir) throws IOException {
        this.dir = dir;
        Files.createDirectories(dir);
        Files.createDirectories(indexDir());
        if (Files.exists(metadataPath())) loadMetadata();
    }

    public Path dir() { return dir; }
    public Catalog catalog() { return catalog; }
    public void resetStats() { rowsRead = 0; }

    public Path metadataPath() { return dir.resolve("metadata.txt"); }
    public Path tablePath(String table) { return dir.resolve(table.toLowerCase() + ".csv"); }
    private Path indexDir() { return dir.resolve("indexes"); }
    public Path indexPath(String index) { return indexDir().resolve(index.toLowerCase() + ".idx"); }

    // ── Metadatos ───────────────────────────────────────────────────────────
    private void loadMetadata() throws IOException {
        TableSchema current = null;
        List<String[]> pendingIndexes = new ArrayList<>();
        for (String raw : Files.readAllLines(metadataPath(), StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            switch (p[0]) {
                case "TABLE":
                    current = new TableSchema(p[1]);
                    catalog.addTable(current);
                    break;
                case "COLUMN": {
                    String t = p[2];
                    int size = 0;
                    int paren = t.indexOf('(');
                    if (paren >= 0) { size = Integer.parseInt(t.substring(paren + 1, t.length() - 1)); t = t.substring(0, paren); }
                    boolean pk = false, nn = false;
                    for (int i = 3; i < p.length; i++) {
                        if (p[i].equals("PRIMARY_KEY")) pk = true;
                        if (p[i].equals("NOT_NULL")) nn = true;
                    }
                    current.columns.add(new TableSchema.Column(p[1], SqlType.valueOf(t), size, pk, nn));
                    break;
                }
                case "INDEX":
                    pendingIndexes.add(p);
                    break;
                default:
                    throw new IOException("metadata.txt: línea desconocida: " + line);
            }
        }
        for (String[] p : pendingIndexes) catalog.addIndex(new Catalog.IndexDef(p[1], p[2], p[3]));
    }

    private void saveMetadata() throws IOException {
        StringBuilder sb = new StringBuilder("# Esquema de la base de datos\n");
        for (TableSchema t : catalog.tables()) {
            sb.append("TABLE ").append(t.name).append('\n');
            for (TableSchema.Column c : t.columns) {
                sb.append("COLUMN ").append(c.name).append(' ').append(c.typeText());
                if (c.primaryKey) sb.append(" PRIMARY_KEY");
                else if (c.notNull) sb.append(" NOT_NULL");
                sb.append('\n');
            }
        }
        for (Catalog.IndexDef i : catalog.indexes())
            sb.append("INDEX ").append(i.name).append(' ').append(i.table).append(' ').append(i.column).append('\n');
        Files.write(metadataPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ── DDL ─────────────────────────────────────────────────────────────────
    public void createTable(TableSchema t) throws IOException {
        catalog.addTable(t);
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < t.columns.size(); i++) header.append(i > 0 ? "," : "").append(t.columns.get(i).name);
        Files.write(tablePath(t.name), (header + "\n").getBytes(StandardCharsets.UTF_8));
        saveMetadata();
    }

    public void dropTable(String name) throws IOException {
        for (Catalog.IndexDef d : catalog.removeTable(name)) {
            indexCache.remove(d.name.toLowerCase());
            Files.deleteIfExists(indexPath(d.name));
        }
        Files.deleteIfExists(tablePath(name));
        saveMetadata();
    }

    public void createIndex(Catalog.IndexDef def) throws IOException {
        catalog.addIndex(def);
        IndexData data = new IndexData(def);
        TableSchema t = catalog.table(def.table);
        int col = t.indexOf(def.column);
        for (StoredRow r : scan(def.table)) data.add(r.values[col], r.offset);
        indexCache.put(def.name.toLowerCase(), data);
        writeIndex(data);
        saveMetadata();
    }

    public void dropIndex(String name) throws IOException {
        catalog.removeIndex(name);
        indexCache.remove(name.toLowerCase());
        Files.deleteIfExists(indexPath(name));
        saveMetadata();
    }

    // ── Filas ───────────────────────────────────────────────────────────────
    /** Posiciones [inicio, fin) de cada fila de datos (sin la cabecera) dentro de los bytes del .csv. */
    private List<int[]> lineRanges(byte[] data) {
        List<int[]> ranges = new ArrayList<>();
        int start = 0;
        boolean header = true;
        for (int i = 0; i <= data.length; i++) {
            boolean end = i == data.length ? start < data.length : data[i] == 0x0A;
            if (!end) continue;
            if (header) header = false;
            else ranges.add(new int[]{start, i});
            start = i + 1;
        }
        return ranges;
    }

    /** Recorre todo el .csv. */
    public List<StoredRow> scan(String table) throws IOException {
        TableSchema schema = catalog.table(table);
        byte[] data = Files.readAllBytes(tablePath(table));
        List<StoredRow> rows = new ArrayList<>();
        for (int[] r : lineRanges(data))
            rows.add(new StoredRow(r[0], parseLine(new String(data, r[0], r[1] - r[0], StandardCharsets.UTF_8), schema)));
        rowsRead += rows.size();
        return rows;
    }

    /** Lee una sola fila usando su posición, sin recorrer el archivo. */
    public StoredRow readAt(String table, long offset) throws IOException {
        TableSchema schema = catalog.table(table);
        try (RandomAccessFile raf = new RandomAccessFile(tablePath(table).toFile(), "r")) {
            raf.seek(offset);
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            int b;
            while ((b = raf.read()) != -1 && b != '\n') buf.write(b);
            rowsRead++;
            return new StoredRow(offset, parseLine(new String(buf.toByteArray(), StandardCharsets.UTF_8), schema));
        }
    }

    private Object[] parseLine(String line, TableSchema schema) throws IOException {
        if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
        List<Object[]> fields = Values.splitCsv(line);
        if (fields.size() != schema.columns.size())
            throw new IOException("Fila corrupta en " + schema.name + ".csv: se esperaban "
                + schema.columns.size() + " campos y hay " + fields.size() + ": " + line);
        Object[] row = new Object[fields.size()];
        for (int i = 0; i < row.length; i++) {
            try {
                row[i] = Values.fromCsvField((String) fields.get(i)[0], (Boolean) fields.get(i)[1], schema.columns.get(i).type);
            } catch (RuntimeException e) {
                throw new IOException("Valor inválido en " + schema.name + "." + schema.columns.get(i).name + ": " + fields.get(i)[0]);
            }
        }
        return row;
    }

    private String formatLine(Object[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) sb.append(i > 0 ? "," : "").append(Values.toCsvField(row[i]));
        return sb.append('\n').toString();
    }

    /** Agrega una fila al final del .csv y la registra en los índices de la tabla (en memoria). */
    public void insert(String table, Object[] values) throws IOException {
        TableSchema schema = catalog.table(table);
        long offset;
        try (RandomAccessFile raf = new RandomAccessFile(tablePath(table).toFile(), "rw")) {
            offset = raf.length();
            raf.seek(offset);
            raf.write(formatLine(values).getBytes(StandardCharsets.UTF_8));
        }
        for (Catalog.IndexDef d : catalog.indexesOf(table))
            index(d.name).add(values[schema.indexOf(d.column)], offset);
    }

    /**
     * Reescribe el .csv aplicando cambios por posición: updates reemplaza filas y deletes las elimina.
     * Las demás filas se copian tal cual, sin interpretarlas, y los índices se ajustan a las nuevas posiciones.
     */
    public void rewriteRows(String table, Map<Long, Object[]> updates, java.util.Set<Long> deletes) throws IOException {
        TableSchema schema = catalog.table(table);
        byte[] data = Files.readAllBytes(tablePath(table));
        List<int[]> ranges = lineRanges(data);
        int headerEnd = ranges.isEmpty() ? data.length : ranges.get(0)[0];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write(data, 0, headerEnd);
        if (headerEnd > 0 && data[headerEnd - 1] != 0x0A) out.write(0x0A);
        Map<Long, Long> remap = new HashMap<>();
        Map<Long, Long> updatedAt = new HashMap<>();
        for (int[] r : ranges) {
            long old = r[0];
            if (deletes.contains(old)) continue;
            long now = out.size();
            if (updates.containsKey(old)) {
                out.write(formatLine(updates.get(old)).getBytes(StandardCharsets.UTF_8));
                updatedAt.put(old, now);
            } else {
                out.write(data, r[0], r[1] - r[0]);
                out.write(0x0A);
                remap.put(old, now);
            }
        }
        for (Catalog.IndexDef d : catalog.indexesOf(table)) {
            IndexData ix = index(d.name);
            ix.remap(remap);
            int col = schema.indexOf(d.column);
            for (Map.Entry<Long, Object[]> u : updates.entrySet())
                if (updatedAt.containsKey(u.getKey())) ix.add(u.getValue()[col], updatedAt.get(u.getKey()));
        }
        Files.write(tablePath(table), out.toByteArray());
        persistIndexes(table);
    }

    // ── Índices ─────────────────────────────────────────────────────────────
    /** Índice cargado (desde su archivo .idx la primera vez). */
    public IndexData index(String name) throws IOException {
        IndexData data = indexCache.get(name.toLowerCase());
        if (data != null) return data;
        Catalog.IndexDef def = catalog.index(name);
        data = new IndexData(def);
        Path file = indexPath(name);
        if (Files.exists(file)) {
            TableSchema schema = catalog.table(def.table);
            SqlType type = schema.columns.get(schema.indexOf(def.column)).type;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                int tab = line.lastIndexOf('\t');
                Object[] key = Values.splitCsv(line.substring(0, tab)).get(0);
                Object value = Values.fromCsvField((String) key[0], (Boolean) key[1], type);
                for (String off : line.substring(tab + 1).split(",")) data.add(value, Long.parseLong(off));
            }
        }
        indexCache.put(name.toLowerCase(), data);
        return data;
    }

    /** Escribe a disco los índices de la tabla (tras INSERT, UPDATE o DELETE). */
    public void persistIndexes(String table) throws IOException {
        for (Catalog.IndexDef d : catalog.indexesOf(table)) writeIndex(index(d.name));
    }

    private void writeIndex(IndexData data) throws IOException {
        Files.write(indexPath(data.def.name), data.serialize().getBytes(StandardCharsets.UTF_8));
    }
}
