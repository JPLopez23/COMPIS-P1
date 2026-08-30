package gui;

import javax.swing.*;
import java.awt.*;

/** Chip redondeado con un punto de color y texto, para el estado y el conteo de errores. */
public class Pill extends JComponent {
    private String text;
    private Color fg;
    private Color bg;
    private boolean showDot = true;

    public Pill(String text, Color fg, Color bg) {
        this.text = text;
        this.fg = fg;
        this.bg = bg;
        setFont(Theme.ui(Font.BOLD, 12));
        setOpaque(false);
    }

    public void set(String text, Color fg, Color bg) {
        this.text = text;
        this.fg = fg;
        this.bg = bg;
        revalidate();
        repaint();
    }

    public void setShowDot(boolean showDot) {
        this.showDot = showDot;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textW = fm.stringWidth(text);
        int dot = showDot ? 14 : 0;
        return new Dimension(textW + dot + 24, 26);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, h, h);

        FontMetrics fm = g2.getFontMetrics(getFont());
        int x = 12;
        if (showDot) {
            g2.setColor(fg);
            g2.fillOval(x, h / 2 - 4, 8, 8);
            x += 14;
        }
        g2.setColor(fg);
        g2.setFont(getFont());
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, textY);
        g2.dispose();
    }
}
