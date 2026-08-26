package semantic;

/** Reglas de compatibilidad y unificación de tipos. */
public final class TypeUtils {
    private TypeUtils() {}

    public static boolean isNumeric(Type t) {
        return t.equals(PrimitiveType.INTEGER) || t.equals(PrimitiveType.FLOAT);
    }

    /** Indica si un valor de tipo value puede asignarse a un destino de tipo target. */
    public static boolean isAssignable(Type target, Type value) {
        if (target.isError() || value.isError()) return true;
        if (target.equals(value)) return true;
        if (value.equals(PrimitiveType.NULL) && (target instanceof ClassType || target instanceof ArrayType))
            return true;
        if (target.equals(PrimitiveType.FLOAT) && value.equals(PrimitiveType.INTEGER)) return true;
        if (target instanceof ArrayType && value instanceof ArrayType)
            return isAssignable(((ArrayType) target).elem, ((ArrayType) value).elem);
        if (target instanceof ClassType && value instanceof ClassType)
            return ((ClassType) value).isSubclassOf((ClassType) target);
        return false;
    }

    /** Tipo resultante de una operación aritmética entre a y b. */
    public static Type unifyNumeric(Type a, Type b) {
        if (a.isError() || b.isError()) return ErrorType.INSTANCE;
        if (a.equals(PrimitiveType.FLOAT) || b.equals(PrimitiveType.FLOAT)) {
            if (isNumeric(a) && isNumeric(b)) return PrimitiveType.FLOAT;
        }
        if (a.equals(PrimitiveType.INTEGER) && b.equals(PrimitiveType.INTEGER)) return PrimitiveType.INTEGER;
        return ErrorType.INSTANCE;
    }

    public static boolean typesComparable(Type a, Type b) {
        if (a.isError() || b.isError()) return true;
        if (a.equals(b)) return true;
        if (isNumeric(a) && isNumeric(b)) return true;
        boolean aNullable = a instanceof ClassType || a instanceof ArrayType;
        boolean bNullable = b instanceof ClassType || b instanceof ArrayType;
        if (a.equals(PrimitiveType.NULL) && bNullable) return true;
        if (b.equals(PrimitiveType.NULL) && aNullable) return true;
        return false;
    }
}
