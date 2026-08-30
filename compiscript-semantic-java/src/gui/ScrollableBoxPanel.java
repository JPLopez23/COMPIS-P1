package gui;

import javax.swing.*;
import java.awt.*;

/** Panel vertical que se estira al ancho del viewport para que el texto envuelva dentro de un JScrollPane. */
public class ScrollableBoxPanel extends JPanel implements Scrollable {
    public ScrollableBoxPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 100; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
