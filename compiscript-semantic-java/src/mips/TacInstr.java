package mips;

import java.util.ArrayList;
import java.util.List;

/** Instrucción TAC ya analizada. */
final class TacInstr {

    enum Kind {
        LABEL, GOTO, IF, IFFALSE, IFREL,
        COPY, BIN, UNARY, LEN, NEWARRAY, NEW, CLOSURE, EXCEPTION,
        PARAM, CALL, CALLIND, RETURN,
        PRINT, PRINTS, PRINTB,
        PUSHHANDLER, POPHANDLER
    }

    final Kind kind;
    final String source;          // línea original (para comentar el ensamblador)
    String label;                 // LABEL, GOTO, IF*, PUSHHANDLER, CALL (función), CLOSURE (función)
    String op;                    // BIN, IFREL, UNARY
    TacOperand dest;              // destino de una asignación (puede ser temporal, variable, global, campo o índice)
    TacOperand a, b;              // operandos
    int n;                        // nº de parámetros (CALL/CALLIND), nº de elementos (NEWARRAY), bytes (NEW)
    int link = -1;                // niveles de enlace de acceso (CALL, CLOSURE)
    String className;             // NEW

    TacInstr(Kind kind, String source) {
        this.kind = kind;
        this.source = source;
    }

    /** Temporales que esta instrucción lee (incluye bases e índices de accesos y destinos indirectos). */
    List<String> reads() {
        if (readsCache == null) {
            List<String> out = new ArrayList<>();
            if (a != null) a.collectTemps(out);
            if (b != null) b.collectTemps(out);
            if (dest != null && !dest.isTemp()) dest.collectTemps(out);
            readsCache = out;
        }
        return readsCache;
    }

    private List<String> readsCache;

    /** Temporal que esta instrucción define, o null. */
    String writes() {
        return dest != null && dest.isTemp() ? dest.tempName() : null;
    }

    @Override public String toString() { return source; }
}
