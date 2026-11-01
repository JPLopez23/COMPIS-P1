package sql.semantic;

import semantic.SemanticError;
import sql.SqlType;
import sql.ast.Ast;
import sql.storage.Catalog;
import sql.storage.TableSchema;
import sql.storage.Values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Análisis semántico de SQL. Trabaja sobre una copia del catálogo (sin datos) a la que va aplicando
 * los CREATE/DROP del script, de modo que un script completo se valida antes de ejecutarse.
 * Además de reportar errores, anota el AST (tipos, resolución de columnas, * expandidos).
 */
public class SqlSemanticAnalyzer {

    public final List<SemanticError> errors = new ArrayList<>();
    private final Catalog cat;

    /** Ámbito de un SELECT (o de un UPDATE/DELETE): las tablas visibles y el ámbito exterior para subconsultas. */
    private static class Scope {
        final Scope parent;
        final List<Ast.TableRef> tables = new ArrayList<>();
        Scope(Scope parent) { this.parent = parent; }
    }

    public SqlSemanticAnalyzer(Catalog real) {
        this.cat = real.copy();
    }

    private void error(String category, String msg, Ast.Node at) {
        errors.add(new SemanticError(category, msg, at.line, at.col));
    }

    public void analyze(Ast.Script script) {
        for (Ast.Stmt s : script.statements) statement(s);
    }

    private void statement(Ast.Stmt s) {
        if (s instanceof Ast.CreateTable) createTable((Ast.CreateTable) s);
        else if (s instanceof Ast.DropTable) dropTable((Ast.DropTable) s);
        else if (s instanceof Ast.CreateIndex) createIndex((Ast.CreateIndex) s);
        else if (s instanceof Ast.DropIndex) dropIndex((Ast.DropIndex) s);
        else if (s instanceof Ast.Insert) insert((Ast.Insert) s);
        else if (s instanceof Ast.Update) update((Ast.Update) s);
        else if (s instanceof Ast.Delete) delete((Ast.Delete) s);
        else if (s instanceof Ast.Select) select((Ast.Select) s, null);
    }

    // ── DDL ─────────────────────────────────────────────────────────────────
    private void createTable(Ast.CreateTable s) {
        boolean exists = cat.table(s.name) != null;
        if (exists) error("TABLE", "La tabla '" + s.name + "' ya existe", s);
        TableSchema schema = new TableSchema(s.name);
        Set<String> seen = new HashSet<>();
        int pks = 0;
        for (Ast.ColumnDef c : s.columns) {
            if (!seen.add(c.name.toLowerCase())) error("COLUMN", "Columna duplicada '" + c.name + "' en la tabla '" + s.name + "'", c);
            if (c.type == SqlType.VARCHAR && c.size <= 0) error("TYPE", "VARCHAR debe tener una longitud mayor que 0", c);
            if (c.primaryKey && ++pks > 1) error("COLUMN", "La tabla '" + s.name + "' declara más de una PRIMARY KEY", c);
            schema.columns.add(new TableSchema.Column(c.name, c.type, c.size, c.primaryKey, c.notNull));
        }
        if (!exists) cat.addTable(schema);
    }

    private void dropTable(Ast.DropTable s) {
        if (cat.table(s.name) == null) error("TABLE", "La tabla '" + s.name + "' no existe", s);
        else cat.removeTable(s.name);
    }

    private void createIndex(Ast.CreateIndex s) {
        if (cat.index(s.name) != null) error("INDEX", "El índice '" + s.name + "' ya existe", s);
        TableSchema t = cat.table(s.table);
        if (t == null) { error("TABLE", "La tabla '" + s.table + "' no existe", s); return; }
        if (t.indexOf(s.column) < 0) { error("COLUMN", "La columna '" + s.column + "' no existe en la tabla '" + s.table + "'", s); return; }
        if (cat.index(s.name) == null) cat.addIndex(new Catalog.IndexDef(s.name, t.name, t.columns.get(t.indexOf(s.column)).name));
    }

    private void dropIndex(Ast.DropIndex s) {
        if (cat.index(s.name) == null) error("INDEX", "El índice '" + s.name + "' no existe", s);
        else cat.removeIndex(s.name);
    }

    // ── DML ─────────────────────────────────────────────────────────────────
    private void insert(Ast.Insert s) {
        TableSchema t = cat.table(s.table);
        if (t == null) { error("TABLE", "La tabla '" + s.table + "' no existe", s); return; }
        int[] target;
        if (s.columns == null) {
            target = new int[t.columns.size()];
            for (int i = 0; i < target.length; i++) target[i] = i;
        } else {
            target = new int[s.columns.size()];
            Set<Integer> used = new HashSet<>();
            for (int i = 0; i < target.length; i++) {
                target[i] = t.indexOf(s.columns.get(i));
                if (target[i] < 0) error("COLUMN", "La columna '" + s.columns.get(i) + "' no existe en la tabla '" + s.table + "'", s);
                else if (!used.add(target[i])) error("INSERT", "La columna '" + s.columns.get(i) + "' aparece más de una vez en el INSERT", s);
            }
            for (int i = 0; i < t.columns.size(); i++)
                if (!used.contains(i) && t.columns.get(i).notNull)
                    error("INSERT", "La columna '" + t.columns.get(i).name + "' es NOT NULL y no recibe valor", s);
        }
        s.targetColumns = target;
        for (Ast.Row row : s.rows) {
            if (row.values.size() != target.length) {
                error("INSERT", "Se esperaban " + target.length + " valores y hay " + row.values.size(), row);
                continue;
            }
            for (int i = 0; i < target.length; i++) {
                Ast.Expr v = row.values.get(i);
                Scope none = new Scope(null);
                expr(v, none, false);
                if (target[i] >= 0) checkAssignable(v, t.columns.get(target[i]), "INSERT");
            }
        }
    }

    private void update(Ast.Update s) {
        TableSchema t = cat.table(s.table);
        if (t == null) { error("TABLE", "La tabla '" + s.table + "' no existe", s); return; }
        Scope sc = singleTableScope(s.table, t);
        Set<Integer> used = new HashSet<>();
        for (Ast.Assignment a : s.assignments) {
            a.columnIndex = t.indexOf(a.column);
            expr(a.value, sc, false);
            if (a.columnIndex < 0) { error("COLUMN", "La columna '" + a.column + "' no existe en la tabla '" + s.table + "'", a); continue; }
            if (!used.add(a.columnIndex)) error("COLUMN", "La columna '" + a.column + "' se asigna más de una vez", a);
            checkAssignable(a.value, t.columns.get(a.columnIndex), "UPDATE");
        }
        if (s.where != null) condition(s.where, sc, "WHERE");
    }

    private void delete(Ast.Delete s) {
        TableSchema t = cat.table(s.table);
        if (t == null) { error("TABLE", "La tabla '" + s.table + "' no existe", s); return; }
        if (s.where != null) condition(s.where, singleTableScope(s.table, t), "WHERE");
    }

    private Scope singleTableScope(String name, TableSchema t) {
        Scope sc = new Scope(null);
        Ast.TableRef ref = new Ast.TableRef(name, null);
        ref.schema = t;
        sc.tables.add(ref);
        return sc;
    }

    /** Verifica que el valor se pueda guardar en la columna (tipo, NOT NULL, longitud, formato de fecha). */
    private void checkAssignable(Ast.Expr v, TableSchema.Column col, String what) {
        SqlType vt = v.type;
        if (vt == SqlType.UNKNOWN) return;
        String where = " en la columna '" + col.name + "' (" + col.typeText() + ")";
        if (vt == SqlType.NULL) {
            if (col.notNull) error(what, "NULL no se permite" + where + ": es NOT NULL", v);
            return;
        }
        boolean ok;
        switch (col.type) {
            case INT:     ok = vt == SqlType.INT; break;
            case FLOAT:   ok = vt.isNumeric(); break;
            case VARCHAR: ok = vt == SqlType.VARCHAR; break;
            case BOOLEAN: ok = vt == SqlType.BOOLEAN; break;
            case DATE:    ok = vt == SqlType.DATE || (vt == SqlType.VARCHAR && v instanceof Ast.Literal); break;
            default:      ok = false;
        }
        if (!ok) { error("TYPE", "Tipos incompatibles: no se puede guardar un valor " + vt + where, v); return; }
        if (v instanceof Ast.Literal) {
            Object val = ((Ast.Literal) v).value;
            if (col.type == SqlType.DATE && val instanceof String && !Values.isValidDate((String) val))
                error(what, "Fecha inválida " + v.sql() + ": el formato es YYYY-MM-DD" , v);
            if (col.type == SqlType.VARCHAR && val instanceof String && ((String) val).length() > col.size)
                error(what, "El texto " + v.sql() + " excede la longitud máxima " + col.size + where, v);
        }
    }

    // ── SELECT ──────────────────────────────────────────────────────────────
    /** Analiza un SELECT; parent es el ámbito exterior cuando es una subconsulta. */
    private void select(Ast.Select s, Scope parent) {
        Scope sc = new Scope(parent);
        List<Ast.TableRef> refs = s.tables();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < refs.size(); i++) {
            Ast.TableRef r = refs.get(i);
            r.schema = cat.table(r.table);
            if (r.schema == null) error("TABLE", "La tabla '" + r.table + "' no existe", r);
            if (!names.add(r.name().toLowerCase()))
                error("REFERENCE", "Referencia ambigua: la tabla o alias '" + r.name() + "' aparece más de una vez en el FROM", r);
            sc.tables.add(r);
            if (i > 0) {
                Ast.Join j = s.joins.get(i - 1);
                condition(j.on, sc, "ON");
            }
        }

        s.expanded = new ArrayList<>();
        for (Ast.SelectItem item : s.items) expandItem(item, sc, s);

        boolean agg = !s.groupBy.isEmpty() || s.having != null;
        for (Ast.SelectItem it : s.expanded) agg |= containsAggregate(it.expr);
        for (Ast.OrderItem o : s.orderBy) agg |= containsAggregate(o.expr);
        s.aggregated = agg;

        for (Ast.SelectItem it : s.expanded) if (!it.star) expr(it.expr, sc, true);
        if (s.where != null) condition(s.where, sc, "WHERE");
        for (Ast.Expr g : s.groupBy) expr(g, sc, false);
        if (s.having != null) {
            expr(s.having, sc, true);
            requireBoolean(s.having, "HAVING");
        }
        for (Ast.OrderItem o : s.orderBy) {
            o.outputIndex = -1;
            if (o.expr instanceof Ast.ColumnRef && ((Ast.ColumnRef) o.expr).table == null) {
                String n = ((Ast.ColumnRef) o.expr).name;
                for (int i = 0; i < s.expanded.size(); i++) {
                    String alias = s.expanded.get(i).alias;
                    if (alias != null && alias.equalsIgnoreCase(n)) { o.outputIndex = i; break; }
                }
            }
            if (o.outputIndex < 0) expr(o.expr, sc, true);
            else o.expr.type = s.expanded.get(o.outputIndex).expr.type;
        }

        if (agg) {
            Set<String> groups = new HashSet<>();
            for (Ast.Expr g : s.groupBy) groups.add(key(g));
            for (Ast.SelectItem it : s.expanded) checkGrouped(it.expr, groups);
            if (s.having != null) checkGrouped(s.having, groups);
            for (Ast.OrderItem o : s.orderBy) if (o.outputIndex < 0) checkGrouped(o.expr, groups);
        }
    }

    /** Reemplaza * y tabla.* por las columnas correspondientes. */
    private void expandItem(Ast.SelectItem item, Scope sc, Ast.Select s) {
        if (!item.star) { s.expanded.add(item); return; }
        boolean any = false;
        for (Ast.TableRef r : sc.tables) {
            if (item.starTable != null && !r.name().equalsIgnoreCase(item.starTable)) continue;
            any = true;
            if (r.schema == null) continue;
            for (TableSchema.Column c : r.schema.columns) {
                Ast.ColumnRef ref = new Ast.ColumnRef(sc.tables.size() > 1 ? r.name() : null, c.name);
                ref.line = item.line; ref.col = item.col;
                Ast.SelectItem expanded = new Ast.SelectItem(ref, c.name, false, null);
                expanded.line = item.line; expanded.col = item.col;
                s.expanded.add(expanded);
            }
        }
        if (!any) error("REFERENCE", "La tabla o alias '" + item.starTable + "' no está en el FROM", item);
    }

    private boolean containsAggregate(Ast.Expr e) {
        if (e instanceof Ast.Aggregate) return true;
        for (Ast.Node c : e.children()) if (c instanceof Ast.Expr && containsAggregate((Ast.Expr) c)) return true;
        return false;
    }

    /** Clave estructural de una expresión; las columnas se comparan por su resolución, no por cómo se escribieron. */
    private String key(Ast.Expr e) {
        if (e instanceof Ast.ColumnRef) {
            Ast.ColumnRef c = (Ast.ColumnRef) e;
            return "col:" + c.depth + ":" + c.tableIdx + ":" + c.colIdx;
        }
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName()).append(e.label());
        for (Ast.Node c : e.children()) if (c instanceof Ast.Expr) sb.append('(').append(key((Ast.Expr) c)).append(')');
        return sb.toString();
    }

    /** En consultas con agregación o GROUP BY, toda columna debe estar agrupada o dentro de un agregado. */
    private void checkGrouped(Ast.Expr e, Set<String> groups) {
        if (e instanceof Ast.Aggregate) return;
        if (groups.contains(key(e))) return;
        if (e instanceof Ast.ColumnRef) {
            Ast.ColumnRef c = (Ast.ColumnRef) e;
            if (c.depth == 0 && e.type != SqlType.UNKNOWN)
                error("REFERENCE", "Referencia inválida: la columna '" + c.sql() + "' debe aparecer en GROUP BY o dentro de una función de agregación", e);
            return;
        }
        if (e instanceof Ast.InSubquery) { checkGrouped(((Ast.InSubquery) e).operand, groups); return; }
        if (e instanceof Ast.Exists || e instanceof Ast.ScalarSubquery) return;
        for (Ast.Node c : e.children()) if (c instanceof Ast.Expr) checkGrouped((Ast.Expr) c, groups);
    }

    // ── Expresiones ─────────────────────────────────────────────────────────
    private void condition(Ast.Expr e, Scope sc, String clause) {
        expr(e, sc, false);
        requireBoolean(e, clause);
    }

    private void requireBoolean(Ast.Expr e, String clause) {
        if (e.type != SqlType.BOOLEAN && e.type != SqlType.UNKNOWN && e.type != SqlType.NULL)
            error("TYPE", "La condición de " + clause + " debe ser booleana y es " + e.type, e);
    }

    private SqlType expr(Ast.Expr e, Scope sc, boolean allowAgg) {
        SqlType t = infer(e, sc, allowAgg);
        e.type = t;
        return t;
    }

    private SqlType infer(Ast.Expr e, Scope sc, boolean allowAgg) {
        if (e instanceof Ast.Literal) return e.type;
        if (e instanceof Ast.ColumnRef) return resolve((Ast.ColumnRef) e, sc);
        if (e instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary) e;
            SqlType t = expr(u.operand, sc, allowAgg);
            if (u.op.equals("NOT")) {
                if (!isBool(t)) error("TYPE", "NOT requiere un operando booleano y recibe " + t, e);
                return SqlType.BOOLEAN;
            }
            if (!isNum(t)) { error("TYPE", "El signo - requiere un número y recibe " + t, e); return SqlType.UNKNOWN; }
            return t;
        }
        if (e instanceof Ast.Binary) return binary((Ast.Binary) e, sc, allowAgg);
        if (e instanceof Ast.IsNull) { expr(((Ast.IsNull) e).operand, sc, allowAgg); return SqlType.BOOLEAN; }
        if (e instanceof Ast.InList) {
            Ast.InList in = (Ast.InList) e;
            SqlType t = expr(in.operand, sc, allowAgg);
            for (Ast.Expr v : in.values) compatible(t, expr(v, sc, allowAgg), in.operand, v, "IN");
            return SqlType.BOOLEAN;
        }
        if (e instanceof Ast.InSubquery) {
            Ast.InSubquery in = (Ast.InSubquery) e;
            SqlType t = expr(in.operand, sc, allowAgg);
            select(in.query, sc);
            if (in.query.expanded.size() != 1) {
                error("SUBQUERY", "La subconsulta de IN debe devolver una sola columna y devuelve " + in.query.expanded.size(), in.query);
            } else {
                compatible(t, in.query.expanded.get(0).expr.type, in.operand, in.query.expanded.get(0).expr, "IN");
            }
            return SqlType.BOOLEAN;
        }
        if (e instanceof Ast.Exists) {
            select(((Ast.Exists) e).query, sc);
            return SqlType.BOOLEAN;
        }
        if (e instanceof Ast.ScalarSubquery) {
            Ast.Select q = ((Ast.ScalarSubquery) e).query;
            select(q, sc);
            if (q.expanded.size() != 1) {
                error("SUBQUERY", "Una subconsulta escalar debe devolver una sola columna y devuelve " + q.expanded.size(), q);
                return SqlType.UNKNOWN;
            }
            return q.expanded.get(0).expr.type;
        }
        if (e instanceof Ast.Aggregate) return aggregate((Ast.Aggregate) e, sc, allowAgg);
        return SqlType.UNKNOWN;
    }

    private SqlType aggregate(Ast.Aggregate a, Scope sc, boolean allowAgg) {
        if (!allowAgg) error("AGGREGATE", "La función " + a.fn + " no se permite en esta cláusula (solo en SELECT, HAVING u ORDER BY)", a);
        if (a.arg == null) return SqlType.INT;
        if (containsAggregate(a.arg)) error("AGGREGATE", "No se pueden anidar funciones de agregación", a);
        SqlType t = expr(a.arg, sc, false);
        switch (a.fn) {
            case "COUNT": return SqlType.INT;
            case "SUM":
                if (!isNum(t)) { error("TYPE", "SUM requiere una columna numérica y recibe " + t, a); return SqlType.UNKNOWN; }
                return t == SqlType.FLOAT ? SqlType.FLOAT : SqlType.INT;
            case "AVG":
                if (!isNum(t)) { error("TYPE", "AVG requiere una columna numérica y recibe " + t, a); return SqlType.UNKNOWN; }
                return SqlType.FLOAT;
            default:
                return t;
        }
    }

    private SqlType binary(Ast.Binary b, Scope sc, boolean allowAgg) {
        SqlType l = expr(b.left, sc, allowAgg), r = expr(b.right, sc, allowAgg);
        switch (b.op) {
            case "AND": case "OR":
                if (!isBool(l) || !isBool(r))
                    error("TYPE", b.op + " requiere operandos booleanos y recibe " + l + " y " + r, b);
                return SqlType.BOOLEAN;
            case "+": case "-": case "*": case "/":
                if (!isNum(l) || !isNum(r)) {
                    error("TYPE", "Tipos incompatibles: la operación " + b.op + " requiere números y recibe " + l + " y " + r, b);
                    return SqlType.UNKNOWN;
                }
                if (l == SqlType.UNKNOWN || r == SqlType.UNKNOWN) return SqlType.UNKNOWN;
                return l == SqlType.FLOAT || r == SqlType.FLOAT ? SqlType.FLOAT : SqlType.INT;
            default:
                compatible(l, r, b.left, b.right, b.op);
                return SqlType.BOOLEAN;
        }
    }

    private boolean isNum(SqlType t) { return t.isNumeric() || t == SqlType.UNKNOWN || t == SqlType.NULL; }
    private boolean isBool(SqlType t) { return t == SqlType.BOOLEAN || t == SqlType.UNKNOWN || t == SqlType.NULL; }

    /** Comprueba que dos tipos se puedan comparar. Una fecha se compara con un literal de texto con formato de fecha. */
    private void compatible(SqlType l, SqlType r, Ast.Expr le, Ast.Expr re, String op) {
        if (l == SqlType.UNKNOWN || r == SqlType.UNKNOWN || l == SqlType.NULL || r == SqlType.NULL) return;
        if (l.isNumeric() && r.isNumeric()) return;
        if (l == r) return;
        if (l == SqlType.DATE && r == SqlType.VARCHAR) { checkDateLiteral(re); return; }
        if (r == SqlType.DATE && l == SqlType.VARCHAR) { checkDateLiteral(le); return; }
        error("TYPE", "Tipos incompatibles: no se puede comparar " + l + " con " + r + " (" + op + ")", le);
    }

    private void checkDateLiteral(Ast.Expr e) {
        if (!(e instanceof Ast.Literal)) { error("TYPE", "Tipos incompatibles: una fecha solo se compara con otra fecha o con un literal 'YYYY-MM-DD'", e); return; }
        Object v = ((Ast.Literal) e).value;
        if (!(v instanceof String) || !Values.isValidDate((String) v))
            error("TYPE", "Fecha inválida " + e.sql() + ": el formato es YYYY-MM-DD", e);
    }

    /** Resuelve una columna en el ámbito actual y en los exteriores (subconsultas correlacionadas). */
    private SqlType resolve(Ast.ColumnRef c, Scope sc) {
        int depth = 0;
        boolean unknownTable = false;
        for (Scope s = sc; s != null; s = s.parent, depth++) {
            if (c.table != null) {
                for (int ti = 0; ti < s.tables.size(); ti++) {
                    Ast.TableRef r = s.tables.get(ti);
                    if (!r.name().equalsIgnoreCase(c.table)) continue;
                    if (r.schema == null) return SqlType.UNKNOWN;
                    int ci = r.schema.indexOf(c.name);
                    if (ci < 0) {
                        error("COLUMN", "La columna '" + c.name + "' no existe en la tabla '" + c.table + "'", c);
                        return SqlType.UNKNOWN;
                    }
                    return bind(c, depth, ti, ci, r.schema);
                }
            } else {
                int found = -1, foundCol = -1;
                List<String> where = new ArrayList<>();
                for (int ti = 0; ti < s.tables.size(); ti++) {
                    Ast.TableRef r = s.tables.get(ti);
                    if (r.schema == null) { unknownTable = true; continue; }
                    int ci = r.schema.indexOf(c.name);
                    if (ci >= 0) { if (found < 0) { found = ti; foundCol = ci; } where.add(r.name()); }
                }
                if (where.size() > 1) {
                    error("REFERENCE", "Referencia ambigua: la columna '" + c.name + "' existe en " + String.join(", ", where)
                        + "; usa tabla.columna", c);
                    return SqlType.UNKNOWN;
                }
                if (found >= 0) return bind(c, depth, found, foundCol, s.tables.get(found).schema);
            }
        }
        if (unknownTable) return SqlType.UNKNOWN;
        if (c.table != null) error("REFERENCE", "Referencia inválida: la tabla o alias '" + c.table + "' no está en el FROM", c);
        else error("COLUMN", "La columna '" + c.name + "' no existe", c);
        return SqlType.UNKNOWN;
    }

    private SqlType bind(Ast.ColumnRef c, int depth, int tableIdx, int colIdx, TableSchema schema) {
        c.depth = depth; c.tableIdx = tableIdx; c.colIdx = colIdx;
        return schema.columns.get(colIdx).type;
    }
}
