package sql;

/** Tipos de dato de SQL. NULL es el tipo del literal NULL y UNKNOWN sirve para no encadenar errores. */
public enum SqlType {
    INT, FLOAT, VARCHAR, BOOLEAN, DATE, NULL, UNKNOWN;

    public boolean isNumeric() { return this == INT || this == FLOAT; }
}
