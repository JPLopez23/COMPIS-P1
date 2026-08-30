package gui;

import java.awt.*;

/** Paleta y tipografía de la interfaz, elegidas según la fase del pipeline. */
public enum Theme {
    IDLE    (new Color(134, 134, 139), new Color(246, 246, 248), new Color(230, 230, 234), "Listo"),
    LOADED  (new Color(0, 122, 255),   new Color(234, 244, 255), new Color(200, 226, 255), "Sin compilar"),
    OK      (new Color(29, 163, 76),   new Color(233, 249, 238), new Color(196, 240, 210), "Sin errores"),
    LEXICAL (new Color(181, 137, 0),   new Color(255, 249, 227), new Color(255, 239, 181), "Error léxico"),
    SYNTAX  (new Color(209, 115, 10),  new Color(255, 242, 229), new Color(255, 220, 176), "Error sintáctico"),
    SEMANTIC(new Color(142, 63, 196),  new Color(247, 238, 251), new Color(230, 204, 247), "Error semántico"),
    MIXED   (new Color(220, 38, 38),   new Color(254, 232, 232), new Color(254, 205, 205), "Varios tipos de error");

    public final Color accent;
    public final Color tint;
    public final Color soft;
    public final String label;

    Theme(Color accent, Color tint, Color soft, String label) {
        this.accent = accent;
        this.tint = tint;
        this.soft = soft;
        this.label = label;
    }

    /** Tema que corresponde a la fase del pipeline. */
    public static Theme forPhase(String phase) {
        switch (phase) {
            case "ok":       return OK;
            case "lexical":  return LEXICAL;
            case "syntax":   return SYNTAX;
            case "semantic": return SEMANTIC;
            case "mixed":    return MIXED;
            default:         return IDLE;
        }
    }

    private static final String UI_FAMILY = resolveUiFamily();

    private static String resolveUiFamily() {
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String want : new String[]{"Helvetica Neue", "Helvetica", "Arial"}) {
            for (String have : available) {
                if (have.equalsIgnoreCase(want)) return have;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font ui(int style, int size) {
        return new Font(UI_FAMILY, style, size);
    }

    public static Font mono(int style, int size) {
        return new Font(Font.MONOSPACED, style, size);
    }
}
