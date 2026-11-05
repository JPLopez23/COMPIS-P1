package sql.exec;

import sql.SqlType;
import sql.ast.Ast;
import sql.storage.Catalog;
import sql.storage.Database;
import sql.storage.IndexData;
import sql.storage.TableSchema;
import sql.storage.Values;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Intérprete: ejecuta sentencias ya validadas por el análisis semántico leyendo y escribiendo los
 * archivos de la {@link Database}. Usa los índices en WHERE y en las condiciones de JOIN cuando puede.
 */
public class Executor {

    /** Fila(s) visibles al evaluar una expresión: una por tabla del FROM y el entorno del SELECT exterior. */
    private static final class Env {
        final Object[][] rows; final Env parent;
        Env(Object[][] rows, Env parent) { this.rows = rows; this.parent = parent; }
    }

    /** Condición de WHERE/ON que se puede resolver con un índice. */
    private static final class Plan {
        Catalog.IndexDef index; int column; SqlType columnType;
        String op;                       // =, <, <=, >, >= o IN
        List<Ast.Expr> keys = new ArrayList<>();
        boolean dependsOnLeft;           // la clave usa columnas de tablas anteriores del FROM
        String text;
    }

    private final Database db;
    private final Map<String, List<Database.StoredRow>> scanCache = new HashMap<>();
    private final Map<Ast.Select, List<Object[]>> subqueryCache = new IdentityHashMap<>();
    private final Map<Ast.Select, Boolean> correlated = new IdentityHashMap<>();
    private final Set<String> notes = new LinkedHashSet<>();

    public Executor(Database db) { this.db = db; }

    /** Ejecuta cada sentencia del script en orden; si una falla se lanza la excepción y las siguientes no corren. */
    public List<ExecResult> executeAll(Ast.Script script) throws IOException {
        List<ExecResult> results = new ArrayList<>();
        for (Ast.Stmt s : script.statements) results.add(execute(s));
        return results;
    }

    public ExecResult execute(Ast.Stmt s) throws IOException {
        db.resetStats();
        scanCache.clear();
        subqueryCache.clear();
        correlated.clear();
        notes.clear();
        ExecResult r = new ExecResult(s.label());
        if (s instanceof Ast.CreateTable) createTable((Ast.CreateTable) s, r);
        else if (s instanceof Ast.DropTable) dropTable((Ast.DropTable) s, r);
        else if (s instanceof Ast.CreateIndex) createIndex((Ast.CreateIndex) s, r);
        else if (s instanceof Ast.DropIndex) {
            db.dropIndex(((Ast.DropIndex) s).name);
            r.message = "Índice '" + ((Ast.DropIndex) s).name + "' eliminado";
        } else if (s instanceof Ast.Insert) insert((Ast.Insert) s, r);
        else if (s instanceof Ast.Update) update((Ast.Update) s, r);
        else if (s instanceof Ast.Delete) delete((Ast.Delete) s, r);
        else if (s instanceof Ast.Select) query((Ast.Select) s, r);
        r.rowsRead = db.rowsRead;
        r.notes.addAll(notes);
        return r;
    }

    // ── DDL ─────────────────────────────────────────────────────────────────
    private void createTable(Ast.CreateTable s, ExecResult r) throws IOException {
        TableSchema t = new TableSchema(s.name);
        for (Ast.ColumnDef c : s.columns) t.columns.add(new TableSchema.Column(c.name, c.type, c.size, c.primaryKey, c.notNull));
        for (Ast.ForeignKey fk : s.foreignKeys) {
            TableSchema ref = fk.refTable.equalsIgnoreCase(s.name) ? t : db.catalog().table(fk.refTable);
            t.foreignKeys.add(new TableSchema.ForeignKey(t.columns.get(t.indexOf(fk.column)).name, ref.name,
                ref.columns.get(ref.indexOf(fk.refColumn)).name));
        }
        db.createTable(t);
        r.message = "Tabla '" + s.name + "' creada con " + s.columns.size() + " columnas"
            + (s.foreignKeys.isEmpty() ? "" : " y " + s.foreignKeys.size() + " clave(s) foránea(s)");
    }

    private void dropTable(Ast.DropTable s, ExecResult r) throws IOException {
        List<String> detached = new ArrayList<>();
        for (Map.Entry<TableSchema, TableSchema.ForeignKey> e : db.catalog().referencesTo(s.name))
            if (!detached.contains(e.getKey().name)) detached.add(e.getKey().name);
        db.dropTable(s.name, s.cascade);
        r.message = "Tabla '" + s.name + "' eliminada";
        if (s.cascade && !detached.isEmpty())
            r.message += " (CASCADE: se quitaron las claves foráneas de " + String.join(", ", detached) + ")";
    }

    private void createIndex(Ast.CreateIndex s, ExecResult r) throws IOException {
        TableSchema t = db.catalog().table(s.table);
        String column = t.columns.get(t.indexOf(s.column)).name;
        db.createIndex(new Catalog.IndexDef(s.name, t.name, column));
        r.message = "Índice '" + s.name + "' creado sobre " + t.name + "(" + column + ") con "
            + db.index(s.name).keyCount() + " valores distintos";
    }

    // ── INSERT / UPDATE / DELETE ────────────────────────────────────────────
    private void insert(Ast.Insert s, ExecResult r) throws IOException {
        TableSchema t = db.catalog().table(s.table);
        Env empty = new Env(new Object[0][], null);
        List<Object[]> rows = new ArrayList<>();
        for (Ast.Row row : s.rows) {
            Object[] values = new Object[t.columns.size()];
            for (int k = 0; k < s.targetColumns.length; k++) {
                int col = s.targetColumns[k];
                values[col] = store(eval(row.values.get(k), empty, null), t.columns.get(col), row.values.get(k));
            }
            rows.add(values);
        }
        checkPrimaryKey(t, rows, null, s);
        checkParents(t, rows, s);
        for (Object[] values : rows) db.insert(t.name, values);
        db.persistIndexes(t.name);
        r.message = rows.size() + (rows.size() == 1 ? " fila insertada" : " filas insertadas") + " en '" + t.name + "'";
    }

    private void update(Ast.Update s, ExecResult r) throws IOException {
        TableSchema t = db.catalog().table(s.table);
        Map<Long, Object[]> updates = new LinkedHashMap<>();
        boolean touchesKey = false;
        List<Database.StoredRow> targets = matching(t, s.where);
        for (Database.StoredRow row : targets) {
            Env env = new Env(new Object[][]{row.values}, null);
            Object[] updated = row.values.clone();
            for (Ast.Assignment a : s.assignments)
                updated[a.columnIndex] = store(eval(a.value, env, null), t.columns.get(a.columnIndex), a.value);
            updates.put(row.offset, updated);
        }
        for (Ast.Assignment a : s.assignments) touchesKey |= t.columns.get(a.columnIndex).primaryKey;
        if (touchesKey) checkPrimaryKey(t, new ArrayList<>(updates.values()), updates.keySet(), s);
        checkParents(t, new ArrayList<>(updates.values()), s);
        checkNotReferenced(t, targets, new HashSet<>(), updates, s);
        if (!updates.isEmpty()) db.rewriteRows(t.name, updates, new HashSet<>());
        r.message = updates.size() + (updates.size() == 1 ? " fila actualizada" : " filas actualizadas") + " en '" + t.name + "'";
    }

    private void delete(Ast.Delete s, ExecResult r) throws IOException {
        TableSchema t = db.catalog().table(s.table);
        Set<Long> offsets = new HashSet<>();
        List<Database.StoredRow> targets = matching(t, s.where);
        for (Database.StoredRow row : targets) offsets.add(row.offset);
        checkNotReferenced(t, targets, offsets, null, s);
        if (!offsets.isEmpty()) db.rewriteRows(t.name, new HashMap<>(), offsets);
        r.message = offsets.size() + (offsets.size() == 1 ? " fila eliminada" : " filas eliminadas") + " de '" + t.name + "'";
    }

    /** Filas de una tabla que cumplen el WHERE, usando un índice cuando la condición lo permite. */
    private List<Database.StoredRow> matching(TableSchema t, Ast.Expr where) throws IOException {
        Ast.TableRef ref = new Ast.TableRef(t.name, null);
        Plan plan = findPlan(0, ref, conjuncts(where));
        List<Database.StoredRow> candidates = candidates(ref, plan, new Object[1][], null);
        List<Database.StoredRow> out = new ArrayList<>();
        for (Database.StoredRow row : candidates) {
            if (where == null || isTrue(eval(where, new Env(new Object[][]{row.values}, null), null))) out.add(row);
        }
        return out;
    }

    /** Convierte un valor al tipo de la columna y valida NOT NULL y la longitud de VARCHAR. */
    private Object store(Object v, TableSchema.Column c, Ast.Node at) {
        if (v == null) {
            if (c.notNull) throw new SqlRuntimeException("INSERT", "NULL no se permite en la columna '" + c.name + "'", at);
            return null;
        }
        Object converted = Values.coerce(v, c.type);
        if (converted == null)
            throw new SqlRuntimeException("TYPE", "No se puede guardar " + Values.display(v) + " en la columna '" + c.name + "' (" + c.typeText() + ")", at);
        if (c.type == SqlType.VARCHAR && ((String) converted).length() > c.size)
            throw new SqlRuntimeException("INSERT", "El texto excede la longitud máxima " + c.size + " de la columna '" + c.name + "'", at);
        return converted;
    }

    /** Cada valor de una columna con clave foránea debe existir en la tabla referenciada (o ser NULL). */
    private void checkParents(TableSchema t, List<Object[]> rows, Ast.Node at) throws IOException {
        for (TableSchema.ForeignKey fk : t.foreignKeys) {
            int ci = t.indexOf(fk.column);
            TableSchema parent = db.catalog().table(fk.refTable);
            int pi = parent.indexOf(fk.refColumn);
            boolean self = parent.name.equalsIgnoreCase(t.name);
            for (Object[] row : rows) {
                Object v = row[ci];
                if (v == null) continue;
                boolean found = false;
                if (self) for (Object[] other : rows) if (other[pi] != null && Values.equal(other[pi], v)) { found = true; break; }
                if (!found && !exists(parent, pi, v))
                    throw new SqlRuntimeException("FOREIGN KEY", "Violación de clave foránea: " + t.name + "." + fk.column + " = "
                        + Values.display(v) + " no existe en " + parent.name + "." + fk.refColumn, at);
            }
        }
    }

    private boolean exists(TableSchema table, int col, Object value) throws IOException {
        Catalog.IndexDef idx = db.catalog().indexOn(table.name, table.columns.get(col).name);
        if (idx != null) return !db.index(idx.name).equal(value).isEmpty();
        for (Database.StoredRow r : allRows(table.name))
            if (r.values[col] != null && Values.equal(r.values[col], value)) return true;
        return false;
    }

    /**
     * Antes de borrar filas (newValues == null) o de cambiar su clave (UPDATE), comprueba que ninguna otra fila
     * las referencie. ignore son las posiciones de filas hijas que desaparecen en el mismo DELETE.
     */
    private void checkNotReferenced(TableSchema t, List<Database.StoredRow> rows, Set<Long> ignore,
                                    Map<Long, Object[]> newValues, Ast.Node at) throws IOException {
        for (Map.Entry<TableSchema, TableSchema.ForeignKey> e : db.catalog().allReferencesTo(t.name)) {
            TableSchema child = e.getKey();
            TableSchema.ForeignKey fk = e.getValue();
            int pi = t.indexOf(fk.refColumn), ci = child.indexOf(fk.column);
            boolean self = child.name.equalsIgnoreCase(t.name);
            Catalog.IndexDef idx = db.catalog().indexOn(child.name, child.columns.get(ci).name);
            for (Database.StoredRow row : rows) {
                Object v = row.values[pi];
                if (v == null) continue;
                if (newValues != null) {
                    Object now = newValues.get(row.offset)[pi];
                    if (now != null && Values.equal(now, v)) continue; // la clave no cambió
                }
                boolean referenced = false;
                if (idx != null) {
                    for (Long off : db.index(idx.name).equal(v)) if (!(self && ignore.contains(off))) { referenced = true; break; }
                } else {
                    for (Database.StoredRow c : allRows(child.name))
                        if (c.values[ci] != null && Values.equal(c.values[ci], v) && !(self && ignore.contains(c.offset))) { referenced = true; break; }
                }
                if (referenced)
                    throw new SqlRuntimeException("FOREIGN KEY", "No se puede " + (newValues == null ? "eliminar" : "modificar") + " " + t.name + "."
                        + fk.refColumn + " = " + Values.display(v) + ": la referencia " + child.name + "." + fk.column, at);
            }
        }
    }

    /** Rechaza claves primarias repetidas entre las filas nuevas y las existentes. */
    private void checkPrimaryKey(TableSchema t, List<Object[]> newRows, Set<Long> replaced, Ast.Node at) throws IOException {
        int pk = -1;
        for (int i = 0; i < t.columns.size(); i++) if (t.columns.get(i).primaryKey) pk = i;
        if (pk < 0) return;
        String name = t.columns.get(pk).name;
        Set<Object> seen = new HashSet<>();
        for (Object[] row : newRows)
            if (!seen.add(row[pk]))
                throw new SqlRuntimeException("INSERT", "Clave primaria duplicada: " + name + " = " + Values.display(row[pk]), at);
        Catalog.IndexDef idx = db.catalog().indexOn(t.name, name);
        if (idx != null) {
            IndexData data = db.index(idx.name);
            for (Object v : seen)
                for (Long off : data.equal(v))
                    if (replaced == null || !replaced.contains(off))
                        throw new SqlRuntimeException("INSERT", "Clave primaria duplicada: " + name + " = " + Values.display(v), at);
        } else {
            for (Database.StoredRow row : db.scan(t.name))
                if ((replaced == null || !replaced.contains(row.offset)) && seen.contains(row.values[pk]))
                    throw new SqlRuntimeException("INSERT", "Clave primaria duplicada: " + name + " = " + Values.display(row.values[pk]), at);
        }
    }

    // ── SELECT ──────────────────────────────────────────────────────────────
    private void query(Ast.Select s, ExecResult r) throws IOException {
        r.isQuery = true;
        for (Ast.SelectItem it : s.expanded) r.columns.add(it.outputName());
        r.rows.addAll(runSelect(s, null));
    }

    private List<Database.StoredRow> allRows(String table) throws IOException {
        List<Database.StoredRow> rows = scanCache.get(table.toLowerCase());
        if (rows == null) { rows = db.scan(table); scanCache.put(table.toLowerCase(), rows); }
        return rows;
    }

    /** Ejecuta un SELECT (también las subconsultas) y devuelve sus filas de salida. */
    private List<Object[]> runSelect(Ast.Select s, Env parent) throws IOException {
        List<Ast.TableRef> refs = s.tables();
        int n = refs.size();
        List<Ast.Expr> whereParts = conjuncts(s.where);
        Plan[] plans = new Plan[n];
        for (int i = 0; i < n; i++) {
            List<Ast.Expr> parts = new ArrayList<>(whereParts);
            if (i > 0) parts.addAll(conjuncts(s.joins.get(i - 1).on));
            plans[i] = findPlan(i, refs.get(i), parts);
        }

        // condiciones del WHERE que solo usan las primeras i+1 tablas se aplican apenas se conocen (antes de seguir uniendo)
        List<List<Ast.Expr>> early = new ArrayList<>();
        for (int i = 0; i < n; i++) early.add(new ArrayList<>());
        for (Ast.Expr part : whereParts) {
            int last = lastTable(part);
            if (last < n - 1) early.get(Math.max(last, 0)).add(part);
        }

        Map<Integer, List<Database.StoredRow>> fixed = new HashMap<>();
        List<Object[][]> combos = new ArrayList<>();
        for (Database.StoredRow row : candidatesFor(refs.get(0), plans[0], 0, new Object[n][], parent, fixed)) {
            Object[][] c = new Object[n][];
            c[0] = row.values;
            if (passes(early.get(0), c, parent)) combos.add(c);
        }
        for (int i = 1; i < n; i++) {
            List<Object[][]> next = new ArrayList<>();
            Ast.Expr on = s.joins.get(i - 1).on;
            for (Object[][] combo : combos) {
                for (Database.StoredRow row : candidatesFor(refs.get(i), plans[i], i, combo, parent, fixed)) {
                    Object[][] c = combo.clone();
                    c[i] = row.values;
                    if (isTrue(eval(on, new Env(c, parent), null)) && passes(early.get(i), c, parent)) next.add(c);
                }
            }
            combos = next;
        }

        List<Env> envs = new ArrayList<>();
        for (Object[][] combo : combos) {
            Env env = new Env(combo, parent);
            if (s.where == null || isTrue(eval(s.where, env, null))) envs.add(env);
        }

        List<Object[][]> produced = new ArrayList<>(); // {fila de salida, claves de orden}
        if (!s.aggregated) {
            for (Env env : envs) produced.add(project(s, env, null));
        } else {
            Map<List<Object>, List<Env>> groups = new LinkedHashMap<>();
            for (Env env : envs) {
                List<Object> key = new ArrayList<>();
                for (Ast.Expr g : s.groupBy) key.add(eval(g, env, null));
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(env);
            }
            if (s.groupBy.isEmpty() && groups.isEmpty()) groups.put(new ArrayList<>(), new ArrayList<>());
            for (List<Env> group : groups.values()) {
                Env rep = group.isEmpty() ? new Env(new Object[n][], parent) : group.get(0);
                if (s.having != null && !isTrue(eval(s.having, rep, group))) continue;
                produced.add(project(s, rep, group));
            }
        }

        if (!s.orderBy.isEmpty()) {
            produced.sort((a, b) -> {
                for (int k = 0; k < s.orderBy.size(); k++) {
                    int c = compareNullsFirst(a[1][k], b[1][k]);
                    if (c != 0) return s.orderBy.get(k).desc ? -c : c;
                }
                return 0;
            });
        }
        List<Object[]> rows = new ArrayList<>();
        for (Object[][] p : produced) rows.add(p[0]);
        return rows;
    }

    private boolean passes(List<Ast.Expr> conditions, Object[][] combo, Env parent) throws IOException {
        for (Ast.Expr c : conditions) if (!isTrue(eval(c, new Env(combo, parent), null))) return false;
        return true;
    }

    /** Última tabla del FROM que usa la condición; con subconsultas o agregados se deja para el final. */
    private int lastTable(Ast.Expr e) {
        if (e instanceof Ast.ColumnRef) { Ast.ColumnRef c = (Ast.ColumnRef) e; return c.depth == 0 ? c.tableIdx : -1; }
        if (e instanceof Ast.InSubquery || e instanceof Ast.Exists || e instanceof Ast.ScalarSubquery || e instanceof Ast.Aggregate)
            return Integer.MAX_VALUE;
        int last = -1;
        for (Ast.Node c : e.children()) if (c instanceof Ast.Expr) last = Math.max(last, lastTable((Ast.Expr) c));
        return last;
    }

    /** Calcula la fila de salida y las claves de ORDER BY para un entorno (o un grupo). */
    private Object[][] project(Ast.Select s, Env env, List<Env> group) throws IOException {
        Object[] row = new Object[s.expanded.size()];
        for (int i = 0; i < row.length; i++) row[i] = eval(s.expanded.get(i).expr, env, group);
        Object[] keys = new Object[s.orderBy.size()];
        for (int k = 0; k < keys.length; k++) {
            Ast.OrderItem o = s.orderBy.get(k);
            keys[k] = o.outputIndex >= 0 ? row[o.outputIndex] : eval(o.expr, env, group);
        }
        return new Object[][]{row, keys};
    }

    private int compareNullsFirst(Object a, Object b) {
        if (a == null || b == null) return a == b ? 0 : a == null ? -1 : 1;
        return Values.compare(a, b);
    }

    // ── Uso de índices ──────────────────────────────────────────────────────
    private List<Ast.Expr> conjuncts(Ast.Expr e) {
        List<Ast.Expr> parts = new ArrayList<>();
        collect(e, parts);
        return parts;
    }

    private void collect(Ast.Expr e, List<Ast.Expr> out) {
        if (e == null) return;
        if (e instanceof Ast.Binary && ((Ast.Binary) e).op.equals("AND")) {
            collect(((Ast.Binary) e).left, out);
            collect(((Ast.Binary) e).right, out);
        } else out.add(e);
    }

    /** Busca una condición "columna op clave" cuya columna (de la tabla ti) tenga índice; prefiere la igualdad. */
    private Plan findPlan(int ti, Ast.TableRef ref, List<Ast.Expr> parts) {
        TableSchema schema = db.catalog().table(ref.table);
        if (schema == null) return null;
        Plan best = null;
        for (Ast.Expr e : parts) {
            Plan p = null;
            if (e instanceof Ast.Binary) {
                Ast.Binary b = (Ast.Binary) e;
                if (b.op.equals("=") || b.op.equals("<") || b.op.equals("<=") || b.op.equals(">") || b.op.equals(">="))
                    p = comparisonPlan(ti, ref, schema, b);
            } else if (e instanceof Ast.InList && !((Ast.InList) e).negated) {
                Ast.InList in = (Ast.InList) e;
                p = columnPlan(ti, ref, schema, in.operand, "IN");
                if (p != null) {
                    boolean ok = true;
                    for (Ast.Expr v : in.values) ok &= keyEligible(v, ti);
                    if (ok) { p.keys.addAll(in.values); finish(p, ti, e); } else p = null;
                }
            }
            if (p == null) continue;
            if (best == null || (!best.op.equals("=") && p.op.equals("="))) best = p;
        }
        return best;
    }

    private Plan comparisonPlan(int ti, Ast.TableRef ref, TableSchema schema, Ast.Binary b) {
        Plan p = columnPlan(ti, ref, schema, b.left, b.op);
        if (p != null && keyEligible(b.right, ti)) { p.keys.add(b.right); return finish(p, ti, b); }
        p = columnPlan(ti, ref, schema, b.right, flip(b.op));
        if (p != null && keyEligible(b.left, ti)) { p.keys.add(b.left); return finish(p, ti, b); }
        return null;
    }

    private Plan columnPlan(int ti, Ast.TableRef ref, TableSchema schema, Ast.Expr col, String op) {
        if (!(col instanceof Ast.ColumnRef)) return null;
        Ast.ColumnRef c = (Ast.ColumnRef) col;
        if (c.depth != 0 || c.tableIdx != ti) return null;
        TableSchema.Column column = schema.columns.get(c.colIdx);
        Catalog.IndexDef idx = db.catalog().indexOn(ref.table, column.name);
        if (idx == null) return null;
        Plan p = new Plan();
        p.index = idx; p.column = c.colIdx; p.columnType = column.type; p.op = op;
        return p;
    }

    private Plan finish(Plan p, int ti, Ast.Expr source) {
        for (Ast.Expr k : p.keys) p.dependsOnLeft |= refsLeft(k, ti);
        String text = source.sql();
        p.text = text.startsWith("(") && text.endsWith(")") && source instanceof Ast.Binary ? text.substring(1, text.length() - 1) : text;
        return p;
    }

    private static String flip(String op) {
        switch (op) {
            case "<": return ">";
            case "<=": return ">=";
            case ">": return "<";
            case ">=": return "<=";
            default: return op;
        }
    }

    /** La clave solo puede usar literales, columnas de tablas anteriores del FROM o del SELECT exterior. */
    private boolean keyEligible(Ast.Expr e, int ti) {
        if (e instanceof Ast.Literal) return true;
        if (e instanceof Ast.ColumnRef) { Ast.ColumnRef c = (Ast.ColumnRef) e; return c.depth > 0 || c.tableIdx < ti; }
        if (e instanceof Ast.Unary) return keyEligible(((Ast.Unary) e).operand, ti);
        if (e instanceof Ast.Binary) return keyEligible(((Ast.Binary) e).left, ti) && keyEligible(((Ast.Binary) e).right, ti);
        return false;
    }

    private boolean refsLeft(Ast.Expr e, int ti) {
        if (e instanceof Ast.ColumnRef) { Ast.ColumnRef c = (Ast.ColumnRef) e; return c.depth == 0 && c.tableIdx < ti; }
        for (Ast.Node c : e.children()) if (c instanceof Ast.Expr && refsLeft((Ast.Expr) c, ti)) return true;
        return false;
    }

    private List<Database.StoredRow> candidatesFor(Ast.TableRef ref, Plan plan, int ti, Object[][] partial, Env parent,
                                                   Map<Integer, List<Database.StoredRow>> fixed) throws IOException {
        if (plan != null && !plan.dependsOnLeft && fixed.containsKey(ti)) return fixed.get(ti);
        List<Database.StoredRow> rows = candidates(ref, plan, partial, parent);
        if (plan != null && !plan.dependsOnLeft) fixed.put(ti, rows);
        return rows;
    }

    /** Filas candidatas de una tabla: las que indica el índice, o todas si no hay plan. */
    private List<Database.StoredRow> candidates(Ast.TableRef ref, Plan plan, Object[][] partial, Env parent) throws IOException {
        if (plan == null) return allRows(ref.table);
        Env keyEnv = new Env(partial, parent);
        IndexData data = db.index(plan.index.name);
        List<Long> offsets = new ArrayList<>();
        boolean usable = true;
        if (plan.op.equals("IN")) {
            Set<Long> all = new TreeSet<>();
            for (Ast.Expr k : plan.keys) {
                Object v = Values.coerce(eval(k, keyEnv, null), plan.columnType);
                if (v != null) all.addAll(data.equal(v));
            }
            offsets.addAll(all);
        } else {
            Object raw = eval(plan.keys.get(0), keyEnv, null);
            if (raw == null) {
                // comparar con NULL nunca es verdadero
            } else {
                Object v = Values.coerce(raw, plan.columnType);
                if (v == null) {
                    if (plan.op.equals("=")) { /* ningún valor de la columna puede ser igual */ }
                    else usable = false;
                } else {
                    switch (plan.op) {
                        case "=":  offsets.addAll(data.equal(v)); break;
                        case "<":  offsets.addAll(data.range(null, false, v, false)); break;
                        case "<=": offsets.addAll(data.range(null, false, v, true)); break;
                        case ">":  offsets.addAll(data.range(v, false, null, false)); break;
                        default:   offsets.addAll(data.range(v, true, null, false)); break;
                    }
                }
            }
        }
        if (!usable) return allRows(ref.table);
        java.util.Collections.sort(offsets);
        notes.add("Índice '" + plan.index.name + "' usado en " + ref.table + "." + plan.index.column + ": " + plan.text);
        List<Database.StoredRow> rows = new ArrayList<>();
        for (Long off : offsets) rows.add(db.readAt(ref.table, off));
        return rows;
    }

    // ── Evaluación de expresiones ───────────────────────────────────────────
    private static boolean isTrue(Object v) { return Boolean.TRUE.equals(v); }

    /** Evalúa una expresión; group no es null cuando se calculan agregados sobre un grupo de filas. */
    private Object eval(Ast.Expr e, Env env, List<Env> group) throws IOException {
        if (e instanceof Ast.Literal) return ((Ast.Literal) e).value;
        if (e instanceof Ast.ColumnRef) {
            Ast.ColumnRef c = (Ast.ColumnRef) e;
            Env x = env;
            for (int d = 0; d < c.depth; d++) x = x.parent;
            return x.rows[c.tableIdx][c.colIdx];
        }
        if (e instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary) e;
            Object v = eval(u.operand, env, group);
            if (v == null) return null;
            if (u.op.equals("NOT")) return !(Boolean) v;
            return v instanceof Long ? (Object) (-(Long) v) : (Object) (-(Double) v);
        }
        if (e instanceof Ast.Binary) return binary((Ast.Binary) e, env, group);
        if (e instanceof Ast.IsNull) {
            boolean isNull = eval(((Ast.IsNull) e).operand, env, group) == null;
            return ((Ast.IsNull) e).negated ? !isNull : isNull;
        }
        if (e instanceof Ast.InList) {
            Ast.InList in = (Ast.InList) e;
            Object v = eval(in.operand, env, group);
            List<Object> values = new ArrayList<>();
            for (Ast.Expr x : in.values) values.add(eval(x, env, group));
            return membership(v, values, in.negated, in);
        }
        if (e instanceof Ast.InSubquery) {
            Ast.InSubquery in = (Ast.InSubquery) e;
            Object v = eval(in.operand, env, group);
            List<Object> values = new ArrayList<>();
            for (Object[] row : subquery(in.query, env)) values.add(row[0]);
            return membership(v, values, in.negated, in);
        }
        if (e instanceof Ast.Exists) return !subquery(((Ast.Exists) e).query, env).isEmpty();
        if (e instanceof Ast.ScalarSubquery) {
            List<Object[]> rows = subquery(((Ast.ScalarSubquery) e).query, env);
            if (rows.size() > 1)
                throw new SqlRuntimeException("SUBQUERY", "La subconsulta escalar devolvió " + rows.size() + " registros y se esperaba uno solo", e);
            return rows.isEmpty() ? null : rows.get(0)[0];
        }
        if (e instanceof Ast.Aggregate) return aggregate((Ast.Aggregate) e, group);
        throw new IllegalStateException("Expresión no soportada: " + e.label());
    }

    private List<Object[]> subquery(Ast.Select q, Env env) throws IOException {
        Boolean corr = correlated.get(q);
        if (corr == null) { corr = isCorrelated(q, 0); correlated.put(q, corr); }
        if (!corr) {
            List<Object[]> cached = subqueryCache.get(q);
            if (cached == null) { cached = runSelect(q, env); subqueryCache.put(q, cached); }
            return cached;
        }
        return runSelect(q, env);
    }

    /** Una subconsulta es correlacionada si alguna columna suya se resuelve fuera de ella. */
    private boolean isCorrelated(Ast.Node n, int level) {
        if (n instanceof Ast.ColumnRef) return ((Ast.ColumnRef) n).depth > level;
        if (n instanceof Ast.Select) {
            Ast.Select s = (Ast.Select) n;
            for (Ast.SelectItem it : s.expanded) if (isCorrelated(it.expr, level)) return true;
        }
        for (Ast.Node c : n.children()) {
            if (isCorrelated(c, c instanceof Ast.Select ? level + 1 : level)) return true;
        }
        return false;
    }

    private Object membership(Object v, List<Object> values, boolean negated, Ast.Node at) {
        if (v == null) return null;
        boolean sawNull = false, found = false;
        for (Object x : values) {
            if (x == null) { sawNull = true; continue; }
            if (compareOp("=", v, x, at) == Boolean.TRUE) { found = true; break; }
        }
        if (found) return !negated;
        if (sawNull) return null;
        return negated;
    }

    private Object binary(Ast.Binary b, Env env, List<Env> group) throws IOException {
        if (b.op.equals("AND") || b.op.equals("OR")) {
            Object l = eval(b.left, env, group);
            boolean and = b.op.equals("AND");
            if (l != null && (Boolean) l != and) return l; // FALSE AND x = FALSE; TRUE OR x = TRUE
            Object r = eval(b.right, env, group);
            if (r != null && (Boolean) r != and) return r;
            return l == null || r == null ? null : (Object) and;
        }
        Object l = eval(b.left, env, group), r = eval(b.right, env, group);
        switch (b.op) {
            case "+": case "-": case "*": case "/": return arithmetic(b.op, l, r, b);
            default: return compareOp(b.op, l, r, b);
        }
    }

    private Object arithmetic(String op, Object a, Object b, Ast.Node at) {
        if (a == null || b == null) return null;
        if (a instanceof Long && b instanceof Long) {
            long x = (Long) a, y = (Long) b;
            switch (op) {
                case "+": return x + y;
                case "-": return x - y;
                case "*": return x * y;
                default:
                    if (y == 0) throw new SqlRuntimeException("ARITHMETIC", "División entre cero", at);
                    return x / y;
            }
        }
        double x = ((Number) a).doubleValue(), y = ((Number) b).doubleValue();
        switch (op) {
            case "+": return x + y;
            case "-": return x - y;
            case "*": return x * y;
            default:
                if (y == 0) throw new SqlRuntimeException("ARITHMETIC", "División entre cero", at);
                return x / y;
        }
    }

    private Boolean compareOp(String op, Object a, Object b, Ast.Node at) {
        if (a == null || b == null) return null;
        int c;
        try {
            c = Values.compare(a, b);
        } catch (IllegalArgumentException ex) {
            throw new SqlRuntimeException("TYPE", ex.getMessage(), at);
        }
        switch (op) {
            case "=": return c == 0;
            case "<>": return c != 0;
            case "<": return c < 0;
            case "<=": return c <= 0;
            case ">": return c > 0;
            default: return c >= 0;
        }
    }

    private Object aggregate(Ast.Aggregate a, List<Env> group) throws IOException {
        if (group == null) throw new SqlRuntimeException("AGGREGATE", "La función " + a.fn + " no se puede usar aquí", a);
        if (a.arg == null) return (long) group.size();
        List<Object> vals = new ArrayList<>();
        for (Env g : group) {
            Object v = eval(a.arg, g, null);
            if (v != null) vals.add(v);
        }
        switch (a.fn) {
            case "COUNT":
                return (long) vals.size();
            case "SUM": {
                if (vals.isEmpty()) return null;
                if (a.type == SqlType.INT) { long s = 0; for (Object v : vals) s += (Long) v; return s; }
                double s = 0; for (Object v : vals) s += ((Number) v).doubleValue();
                return s;
            }
            case "AVG": {
                if (vals.isEmpty()) return null;
                double s = 0; for (Object v : vals) s += ((Number) v).doubleValue();
                return s / vals.size();
            }
            default: {
                if (vals.isEmpty()) return null;
                Object best = vals.get(0);
                for (Object v : vals) {
                    int c = Values.compare(v, best);
                    if (a.fn.equals("MIN") ? c < 0 : c > 0) best = v;
                }
                return best;
            }
        }
    }
}
