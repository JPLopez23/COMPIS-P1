package sql.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Índice ordenado de una columna: valor → posiciones (offsets en bytes) de las filas dentro del .csv.
 * Los NULL no se indexan. Se guarda en database/indexes/NOMBRE.idx.
 */
public class IndexData {
    public final Catalog.IndexDef def;
    private final TreeMap<Object, List<Long>> entries = new TreeMap<>(Values::compare);

    public IndexData(Catalog.IndexDef def) { this.def = def; }

    public void add(Object key, long offset) {
        if (key == null) return;
        entries.computeIfAbsent(key, k -> new ArrayList<>()).add(offset);
    }

    public void remove(Object key, long offset) {
        if (key == null) return;
        List<Long> list = entries.get(key);
        if (list == null) return;
        list.remove(offset);
        if (list.isEmpty()) entries.remove(key);
    }

    public void clear() { entries.clear(); }

    /** Cambia las posiciones según el mapa (antigua → nueva) y descarta las que no aparecen en él. */
    public void remap(Map<Long, Long> moved) {
        java.util.Iterator<Map.Entry<Object, List<Long>>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            List<Long> list = it.next().getValue();
            List<Long> updated = new ArrayList<>();
            for (Long off : list) { Long n = moved.get(off); if (n != null) updated.add(n); }
            if (updated.isEmpty()) it.remove();
            else { list.clear(); list.addAll(updated); }
        }
    }

    public int keyCount() { return entries.size(); }

    public List<Long> equal(Object key) {
        List<Long> l = entries.get(key);
        return l == null ? new ArrayList<>() : new ArrayList<>(l);
    }

    /** Offsets de las claves en el rango; from/to pueden ser null (sin límite). */
    public List<Long> range(Object from, boolean fromInclusive, Object to, boolean toInclusive) {
        Map<Object, List<Long>> view;
        if (from == null && to == null) view = entries;
        else if (from == null) view = entries.headMap(to, toInclusive);
        else if (to == null) view = entries.tailMap(from, fromInclusive);
        else {
            if (Values.compare(from, to) > 0) return new ArrayList<>();
            view = entries.subMap(from, fromInclusive, to, toInclusive);
        }
        List<Long> out = new ArrayList<>();
        for (List<Long> l : view.values()) out.addAll(l);
        return out;
    }

    /** Contenido del archivo .idx: una cabecera y una línea "clave TAB offsets" por cada valor distinto. */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("# INDEX ").append(def.name).append(" ON ").append(def.table).append('(').append(def.column).append(")\n");
        for (Map.Entry<Object, List<Long>> e : entries.entrySet()) {
            sb.append(Values.toCsvField(e.getKey())).append('\t');
            for (int i = 0; i < e.getValue().size(); i++) sb.append(i > 0 ? "," : "").append(e.getValue().get(i));
            sb.append('\n');
        }
        return sb.toString();
    }
}
