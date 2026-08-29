package gui;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;

/** Recoge los errores del lexer o del parser como SemanticError, con su categoría. */
public class CollectingErrorListener extends BaseErrorListener {
    public final String category;
    public final List<SemanticError> errors = new ArrayList<>();

    public CollectingErrorListener(String category) {
        this.category = category;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        errors.add(new SemanticError(category, msg, line, charPositionInLine + 1));
    }
}
