package gui;


import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/** Panel que acepta un archivo por drag-and-drop o por selector, y muestra su nombre. */
public class DropZone extends JPanel {

    private static final Color BG_EMPTY  = new Color(248, 250, 252);
    private static final Color BG_LOADED = new Color(240, 253, 244);
    private static final Color BG_DRAG   = new Color(219, 234, 254);
    private static final Color C_BORDER_EMPTY  = new Color(203, 213, 225);
    private static final Color C_BORDER_LOADED = new Color(74, 175, 96);
    private static final Color C_BORDER_DRAG   = new Color(96, 165, 250);
    private static final Color C_HINT   = new Color(148, 163, 184);
    private static final Color C_LOADED = new Color(21, 128, 61);

    private final String   zoneLabel;
    private final String[] extensions;

    private File     currentFile;
    private Runnable onFileChanged;

    private final JLabel iconLabel;
    private final JLabel hintLabel;
    private final JLabel fileLabel;
    private final JButton browseBtn;

    public DropZone(String label, String... extensions) {
        this.zoneLabel  = label;
        this.extensions = extensions;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(210, 140));
        setMaximumSize(new Dimension(230, 140));
        setBackground(BG_EMPTY);
        setBorder(dashedBorder(C_BORDER_EMPTY));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(new Color(51, 65, 85));
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));

        iconLabel = new JLabel("⬇");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 26));
        iconLabel.setForeground(C_HINT);
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);

        hintLabel = new JLabel("Arrastra aquí");
        hintLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hintLabel.setForeground(C_HINT);
        hintLabel.setAlignmentX(CENTER_ALIGNMENT);

        fileLabel = new JLabel(" ");
        fileLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        fileLabel.setForeground(C_LOADED);
        fileLabel.setAlignmentX(CENTER_ALIGNMENT);
        fileLabel.setVisible(false);

        browseBtn = new JButton("Elegir archivo");
        browseBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        browseBtn.setAlignmentX(CENTER_ALIGNMENT);
        browseBtn.setFocusPainted(false);
        browseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        browseBtn.addActionListener(e -> browse());

        add(titleLabel);
        add(iconLabel);
        add(Box.createVerticalStrut(3));
        add(hintLabel);
        add(fileLabel);
        add(Box.createVerticalStrut(6));
        add(browseBtn);
        add(Box.createVerticalStrut(10));

        setupDrop();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) browse();
            }
        });
    }

    // ── Drag & Drop ──────────────────────────────────────────────────────────
    private void setupDrop() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {

            @Override
            public void dragEnter(DropTargetDragEvent e) {
                e.acceptDrag(DnDConstants.ACTION_COPY);
                setBackground(BG_DRAG);
                setBorder(dashedBorder(C_BORDER_DRAG));
            }

            @Override
            public void dragOver(DropTargetDragEvent e) {
                e.acceptDrag(DnDConstants.ACTION_COPY);
            }

            @Override
            public void dragExit(DropTargetEvent e) {
                resetVisual();
            }

            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable t = e.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>)
                            t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            setFile(files.get(0));
                            e.dropComplete(true);
                            return;
                        }
                    }
                    e.dropComplete(false);
                } catch (Exception ex) {
                    e.dropComplete(false);
                }
                resetVisual();
            }

            @Override public void dropActionChanged(DropTargetDragEvent e) {}
        });
    }

    // ── Selector de archivo ──────────────────────────────────────────────────
    private void browse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar " + zoneLabel);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String n = f.getName().toLowerCase();
                for (String ext : extensions) if (n.endsWith(ext)) return true;
                return false;
            }
            @Override public String getDescription() {
                return zoneLabel + " (" + String.join(", ", extensions) + ")";
            }
        });
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            setFile(chooser.getSelectedFile());
    }

    // ── API pública ──────────────────────────────────────────────────────────
    public void setFile(File file) {
        this.currentFile = file;
        iconLabel.setText("✓");
        iconLabel.setForeground(C_LOADED);
        hintLabel.setVisible(false);
        fileLabel.setText(truncate(file.getName(), 22));
        fileLabel.setVisible(true);
        browseBtn.setText("Cambiar");
        setBackground(BG_LOADED);
        setBorder(BorderFactory.createLineBorder(C_BORDER_LOADED, 2));
        repaint();
        if (onFileChanged != null) onFileChanged.run();
    }

    public void setOnFileChanged(Runnable r) { this.onFileChanged = r; }
    public File    getFile()    { return currentFile; }
    public boolean hasFile()    { return currentFile != null; }

    // ── Utilidades internas ──────────────────────────────────────────────────
    private void resetVisual() {
        if (currentFile != null) {
            setBackground(BG_LOADED);
            setBorder(BorderFactory.createLineBorder(C_BORDER_LOADED, 2));
        } else {
            setBackground(BG_EMPTY);
            setBorder(dashedBorder(C_BORDER_EMPTY));
        }
        repaint();
    }

    private static Border dashedBorder(Color c) {
        return BorderFactory.createDashedBorder(c, 6f, 3f);
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
