package sql.exec;

import sql.ast.Ast;

/** Error detectado al ejecutar (por ejemplo, una subconsulta escalar con varias filas o una clave duplicada). */
public class SqlRuntimeException extends RuntimeException {
    public final String category;
    public final int line, col;

    public SqlRuntimeException(String category, String message, Ast.Node at) {
        super(message);
        this.category = category;
        this.line = at == null ? 0 : at.line;
        this.col = at == null ? 0 : at.col;
    }
}
