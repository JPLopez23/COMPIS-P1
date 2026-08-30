package gui;

import javax.swing.*;
import java.awt.*;

/** Botón de esquinas redondeadas con fondo sólido recoloreable con setAccent. */
public class RoundedButton extends JButton {
    private Color accent;

    public RoundedButton(String text, Color accent) {
        super(text);
        this.accent = accent;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setFont(Theme.ui(Font.BOLD, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
    }

    public void setAccent(Color c) {
        this.accent = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight(), w = getWidth();
        Color fill = accent;
        if (getModel().isPressed()) fill = fill.darker();
        else if (getModel().isRollover()) fill = brighten(fill, 0.08f);
        if (!isEnabled()) fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 110);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, h, h);
        g2.dispose();
        super.paintComponent(g);
    }

    private static Color brighten(Color c, float amount) {
        int r = Math.min(255, (int) (c.getRed() + 255 * amount));
        int g = Math.min(255, (int) (c.getGreen() + 255 * amount));
        int b = Math.min(255, (int) (c.getBlue() + 255 * amount));
        return new Color(r, g, b);
    }
}
