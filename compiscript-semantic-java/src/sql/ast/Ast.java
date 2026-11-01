package sql.ast;

import sql.SqlType;
import sql.storage.TableSchema;

import java.util.ArrayList;
import java.util.List;

/** Nodos del AST de SQL. Cada nodo sabe su posición, su etiqueta y sus hijos (para dibujarlo). */
public final class Ast {
    private Ast() {}

    public abstract static class Node {
        public int line, col;
        public abstract String label();
        public List<Node> children() { return List.of(); }
    }

    public abstract static class Stmt extends Node {}

    public abstract static class Expr extends Node {
        /** Tipo inferido por el análisis semántico. */
        public SqlType type = SqlType.UNKNOWN;
        /** Texto SQL equivalente; sirve como nombre de columna en los resultados. */
        public abstract String sql();
    }

    // ── Programa ────────────────────────────────────────────────────────────
    public static class Script extends Node {
        public final List<Stmt> statements = new ArrayList<>();
        public String label() { return "Script"; }
        public List<Node> children() { return new ArrayList<>(statements); }
    }

    // ── DDL ─────────────────────────────────────────────────────────────────
    public static class ColumnDef extends Node {
        public final String name; public final SqlType type; public final int size;
        public final boolean primaryKey, notNull;
        public ColumnDef(String name, SqlType type, int size, boolean primaryKey, boolean notNull) {
            this.name = name; this.type = type; this.size = size; this.primaryKey = primaryKey; this.notNull = notNull;
        }
        public String label() {
            return "Column " + name + " " + type + (type == SqlType.VARCHAR ? "(" + size + ")" : "")
                + (primaryKey ? " PRIMARY KEY" : "") + (notNull ? " NOT NULL" : "");
        }
    }

    public static class CreateTable extends Stmt {
        public final String name; public final List<ColumnDef> columns;
        public CreateTable(String name, List<ColumnDef> columns) { this.name = name; this.columns = columns; }
        public String label() { return "CreateTable " + name; }
        public List<Node> children() { return new ArrayList<>(columns); }
    }

    public static class DropTable extends Stmt {
        public final String name;
        public DropTable(String name) { this.name = name; }
        public String label() { return "DropTable " + name; }
    }

    public static class CreateIndex extends Stmt {
        public final String name, table, column;
        public CreateIndex(String name, String table, String column) { this.name = name; this.table = table; this.column = column; }
        public String label() { return "CreateIndex " + name + " ON " + table + "(" + column + ")"; }
    }

    public static class DropIndex extends Stmt {
        public final String name;
        public DropIndex(String name) { this.name = name; }
        public String label() { return "DropIndex " + name; }
    }

    // ── DML ─────────────────────────────────────────────────────────────────
    public static class Row extends Node {
        public final List<Expr> values;
        public Row(List<Expr> values) { this.values = values; }
        public String label() { return "Row"; }
        public List<Node> children() { return new ArrayList<>(values); }
    }

    public static class Insert extends Stmt {
        public final String table; public final List<String> columns; public final List<Row> rows;
        /** Posición en el esquema de la columna a la que va cada valor (lo llena el análisis semántico). */
        public int[] targetColumns;
        public Insert(String table, List<String> columns, List<Row> rows) { this.table = table; this.columns = columns; this.rows = rows; }
        public String label() { return "Insert " + table + (columns == null ? "" : " " + columns); }
        public List<Node> children() { return new ArrayList<>(rows); }
    }

    public static class Assignment extends Node {
        public final String column; public final Expr value;
        public int columnIndex;
        public Assignment(String column, Expr value) { this.column = column; this.value = value; }
        public String label() { return "Set " + column; }
        public List<Node> children() { return List.of(value); }
    }

    public static class Update extends Stmt {
        public final String table; public final List<Assignment> assignments; public final Expr where;
        public Update(String table, List<Assignment> assignments, Expr where) { this.table = table; this.assignments = assignments; this.where = where; }
        public String label() { return "Update " + table; }
        public List<Node> children() {
            List<Node> c = new ArrayList<>(assignments);
            if (where != null) c.add(new Wrapper("Where", where));
            return c;
        }
    }

    public static class Delete extends Stmt {
        public final String table; public final Expr where;
        public Delete(String table, Expr where) { this.table = table; this.where = where; }
        public String label() { return "Delete " + table; }
        public List<Node> children() { return where == null ? List.of() : List.of(new Wrapper("Where", where)); }
    }

    // ── SELECT ──────────────────────────────────────────────────────────────
    public static class TableRef extends Node {
        public final String table, alias;
        public TableSchema schema;
        public TableRef(String table, String alias) { this.table = table; this.alias = alias; }
        public String name() { return alias != null ? alias : table; }
        public String label() { return "Table " + table + (alias != null ? " AS " + alias : ""); }
    }

    public static class Join extends Node {
        public final TableRef table; public final Expr on;
        public Join(TableRef table, Expr on) { this.table = table; this.on = on; }
        public String label() { return "InnerJoin"; }
        public List<Node> children() { return List.of(table, new Wrapper("On", on)); }
    }

    /** Elemento de la lista del SELECT; si es * (o alias.*) la expresión se llena al expandirlo. */
    public static class SelectItem extends Node {
        public Expr expr; public String alias;
        public final boolean star; public final String starTable;
        public SelectItem(Expr expr, String alias, boolean star, String starTable) {
            this.expr = expr; this.alias = alias; this.star = star; this.starTable = starTable;
        }
        public String outputName() {
            if (alias != null) return alias;
            return expr instanceof ColumnRef ? ((ColumnRef) expr).name : expr.sql();
        }
        public String label() {
            if (star) return "Item " + (starTable == null ? "*" : starTable + ".*");
            return "Item" + (alias != null ? " AS " + alias : "");
        }
        public List<Node> children() { return expr == null || star ? List.of() : List.of(expr); }
    }

    public static class OrderItem extends Node {
        public final Expr expr; public final boolean desc;
        /** Si la expresión es el alias de una columna del SELECT, su posición; si no, -1. */
        public int outputIndex = -1;
        public OrderItem(Expr expr, boolean desc) { this.expr = expr; this.desc = desc; }
        public String label() { return "Order " + (desc ? "DESC" : "ASC"); }
        public List<Node> children() { return List.of(expr); }
    }

    public static class Select extends Stmt {
        public final List<SelectItem> items; public final TableRef from; public final List<Join> joins;
        public final Expr where; public final List<Expr> groupBy; public final Expr having; public final List<OrderItem> orderBy;
        /** Lista del SELECT con los * ya expandidos (la llena el análisis semántico). */
        public List<SelectItem> expanded;
        public boolean aggregated;
        public Select(List<SelectItem> items, TableRef from, List<Join> joins, Expr where,
                      List<Expr> groupBy, Expr having, List<OrderItem> orderBy) {
            this.items = items; this.from = from; this.joins = joins; this.where = where;
            this.groupBy = groupBy; this.having = having; this.orderBy = orderBy;
        }
        public List<TableRef> tables() {
            List<TableRef> t = new ArrayList<>();
            t.add(from);
            for (Join j : joins) t.add(j.table);
            return t;
        }
        public String label() { return "Select"; }
        public List<Node> children() {
            List<Node> c = new ArrayList<>();
            c.add(new Wrapper("Items", items));
            c.add(new Wrapper("From", from));
            c.addAll(joins);
            if (where != null) c.add(new Wrapper("Where", where));
            if (!groupBy.isEmpty()) c.add(new Wrapper("GroupBy", groupBy));
            if (having != null) c.add(new Wrapper("Having", having));
            if (!orderBy.isEmpty()) c.add(new Wrapper("OrderBy", orderBy));
            return c;
        }
    }

    /** Nodo auxiliar para agrupar hijos con un rótulo (Where, Items, ...). */
    public static class Wrapper extends Node {
        private final String label; private final List<? extends Node> kids;
        public Wrapper(String label, Node kid) { this.label = label; this.kids = List.of(kid); }
        public Wrapper(String label, List<? extends Node> kids) { this.label = label; this.kids = kids; }
        public String label() { return label; }
        public List<Node> children() { return new ArrayList<>(kids); }
    }

    // ── Expresiones ─────────────────────────────────────────────────────────
    public static class Literal extends Expr {
        /** Long, Double, String, Boolean o null (NULL). */
        public final Object value; public final String text;
        public Literal(Object value, SqlType type, String text) { this.value = value; this.type = type; this.text = text; }
        public String label() { return "Literal " + text + " : " + type; }
        public String sql() { return text; }
    }

    public static class ColumnRef extends Expr {
        public final String table, name;
        /** Resolución: cuántos SELECT hacia afuera está la tabla, posición de la tabla en el FROM y de la columna. */
        public int depth, tableIdx, colIdx;
        public ColumnRef(String table, String name) { this.table = table; this.name = name; }
        public String label() { return "Column " + sql() + (type != SqlType.UNKNOWN ? " : " + type : ""); }
        public String sql() { return table == null ? name : table + "." + name; }
    }

    public static class Unary extends Expr {
        public final String op; public final Expr operand;
        public Unary(String op, Expr operand) { this.op = op; this.operand = operand; }
        public String label() { return "Unary " + op + " : " + type; }
        public List<Node> children() { return List.of(operand); }
        public String sql() { return op.equals("NOT") ? "NOT " + operand.sql() : "-" + operand.sql(); }
    }

    public static class Binary extends Expr {
        public final String op; public final Expr left, right;
        public Binary(String op, Expr left, Expr right) { this.op = op; this.left = left; this.right = right; }
        public String label() { return "Binary " + op + " : " + type; }
        public List<Node> children() { return List.of(left, right); }
        public String sql() { return "(" + left.sql() + " " + op + " " + right.sql() + ")"; }
    }

    public static class IsNull extends Expr {
        public final Expr operand; public final boolean negated;
        public IsNull(Expr operand, boolean negated) { this.operand = operand; this.negated = negated; }
        public String label() { return negated ? "IsNotNull" : "IsNull"; }
        public List<Node> children() { return List.of(operand); }
        public String sql() { return operand.sql() + (negated ? " IS NOT NULL" : " IS NULL"); }
    }

    public static class InList extends Expr {
        public final Expr operand; public final List<Expr> values; public final boolean negated;
        public InList(Expr operand, List<Expr> values, boolean negated) { this.operand = operand; this.values = values; this.negated = negated; }
        public String label() { return negated ? "NotIn" : "In"; }
        public List<Node> children() { List<Node> c = new ArrayList<>(); c.add(operand); c.addAll(values); return c; }
        public String sql() {
            StringBuilder sb = new StringBuilder(operand.sql()).append(negated ? " NOT IN (" : " IN (");
            for (int i = 0; i < values.size(); i++) sb.append(i > 0 ? ", " : "").append(values.get(i).sql());
            return sb.append(")").toString();
        }
    }

    public static class InSubquery extends Expr {
        public final Expr operand; public final Select query; public final boolean negated;
        public InSubquery(Expr operand, Select query, boolean negated) { this.operand = operand; this.query = query; this.negated = negated; }
        public String label() { return negated ? "NotInSubquery" : "InSubquery"; }
        public List<Node> children() { return List.of(operand, query); }
        public String sql() { return operand.sql() + (negated ? " NOT IN (subconsulta)" : " IN (subconsulta)"); }
    }

    public static class Exists extends Expr {
        public final Select query;
        public Exists(Select query) { this.query = query; }
        public String label() { return "Exists"; }
        public List<Node> children() { return List.of(query); }
        public String sql() { return "EXISTS (subconsulta)"; }
    }

    public static class ScalarSubquery extends Expr {
        public final Select query;
        public ScalarSubquery(Select query) { this.query = query; }
        public String label() { return "ScalarSubquery : " + type; }
        public List<Node> children() { return List.of(query); }
        public String sql() { return "(subconsulta)"; }
    }

    public static class Aggregate extends Expr {
        public final String fn; public final Expr arg; // arg == null: COUNT(*)
        public Aggregate(String fn, Expr arg) { this.fn = fn; this.arg = arg; }
        public String label() { return "Aggregate " + fn + " : " + type; }
        public List<Node> children() { return arg == null ? List.of() : List.of(arg); }
        public String sql() { return fn + "(" + (arg == null ? "*" : arg.sql()) + ")"; }
    }

    /** Dibuja un nodo y sus descendientes como árbol de texto con sangría. */
    public static String dump(Node root) {
        StringBuilder sb = new StringBuilder();
        dump(root, "", true, true, sb);
        return sb.toString();
    }

    private static void dump(Node n, String prefix, boolean last, boolean root, StringBuilder sb) {
        sb.append(root ? "" : prefix + (last ? "└─ " : "├─ ")).append(n.label()).append('\n');
        List<Node> kids = n.children();
        String next = root ? "" : prefix + (last ? "   " : "│  ");
        for (int i = 0; i < kids.size(); i++) dump(kids.get(i), next, i == kids.size() - 1, false, sb);
    }
}
