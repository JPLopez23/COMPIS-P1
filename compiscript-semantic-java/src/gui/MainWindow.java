package gui;

import semantic.Scope;
import semantic.SemanticError;
import semantic.Symbol;
import viz.GraphvizUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Ventana principal del IDE gráfico de Compiscript. */
public class MainWindow extends JFrame {

    private static final Color C_GRID = new Color(226, 232, 240);
    private static final String GRAMMAR_PATH = "grammar/Compiscript.g4";

    private final DropZone cpsZone = new DropZone("Código  (.cps)", ".cps");

    private final RoundedButton runBtn  = new RoundedButton("▶  Compilar", Theme.IDLE.accent);
    private final JButton newBtn     = new JButton("Nuevo");
    private final JButton saveBtn    = new JButton("Guardar como…");
    private final JButton grammarBtn = new JButton("Ver gramática");
    private final Pill statePill   = new Pill(Theme.IDLE.label, Theme.IDLE.accent, Theme.IDLE.soft);

    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
    private static final int TAB_EDITOR  = 0;
    private static final int TAB_ERRORS  = 1;
    private static final int TAB_TREE    = 2;
    private static final int TAB_SYMBOLS = 3;
    private static final int TAB_TAC     = 4;
    private static final int TAB_FRAMES  = 5;

    private final JTextArea editorArea = new JTextArea();
    private final JTextArea consoleArea = new JTextArea();
    private final JButton clearConsoleBtn = new JButton("Limpiar");
    private JPanel topBar;
    private JPanel consoleHead;
    private JPanel statusBar;

    private Theme currentPhase = Theme.IDLE;
    private boolean suppressDocEvents = true;

    public MainWindow() {
        super("Compiscript IDE  —  Generación de Código Intermedio");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 820);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        add(topBar = buildTopBar(), BorderLayout.NORTH);

        initTabs();
        cpsZone.setOnFileChanged(() -> loadIntoEditor(cpsZone.getFile()));
        tabs.setFont(Theme.ui(Font.PLAIN, 12));
        add(tabs, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(buildConsolePanel(), BorderLayout.CENTER);
        south.add(statusBar = buildStatusBar(), BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        suppressDocEvents = true;
        editorArea.setText(
            "// Escribe tu programa Compiscript aquí\n" +
            "let x: integer = 1;\n" +
            "print(x);\n");
        suppressDocEvents = false;

        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onEdit(); }
            @Override public void removeUpdate(DocumentEvent e) { onEdit(); }
            @Override public void changedUpdate(DocumentEvent e) { onEdit(); }
        });

        applyPhase(Theme.IDLE, Theme.IDLE.label);
    }

    /** Al editar, el resultado mostrado queda obsoleto. */
    private void onEdit() {
        if (suppressDocEvents) return;
        applyPhase(Theme.LOADED, "Modificado");
    }

    // ── Barra superior ───────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_GRID),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        bar.add(cpsZone);
        bar.add(Box.createHorizontalStrut(14));
        bar.add(buildFileButtons());
        bar.add(Box.createHorizontalGlue());
        bar.add(buildRunPanel());

        return bar;
    }

    private JPanel buildFileButtons() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        styleSecondaryButton(newBtn);
        styleSecondaryButton(saveBtn);
        styleSecondaryButton(grammarBtn);
        newBtn.addActionListener(e -> newFile());
        saveBtn.addActionListener(e -> saveAs());
        grammarBtn.addActionListener(e -> viewGrammar());

        p.add(Box.createVerticalGlue());
        p.add(newBtn);
        p.add(Box.createVerticalStrut(6));
        p.add(saveBtn);
        p.add(Box.createVerticalStrut(6));
        p.add(grammarBtn);
        p.add(Box.createVerticalGlue());
        return p;
    }

    /** Abre la gramática en un editor con opción de guardarla y aplicarla al proyecto. */
    private void viewGrammar() {
        JDialog dialog = new JDialog(this, "Gramática", false);
        dialog.setSize(760, 760);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JLabel title = new JLabel();
        title.setFont(Theme.ui(Font.BOLD, 13));
        title.setForeground(Theme.LOADED.accent);

        JTextArea area = new JTextArea();
        area.setFont(Theme.mono(Font.PLAIN, 13));
        area.setEditable(true);
        area.setBackground(new Color(252, 252, 255));
        area.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Archivo cargado en el editor; por defecto la gramática del proyecto.
        File[] current = { new File(GRAMMAR_PATH) };

        JButton openOtherBtn = new JButton("Abrir otra…");
        JButton saveBtn      = new JButton("Guardar");
        JButton useBtn       = new JButton("Usar en el proyecto");
        for (JButton b : new JButton[]{ openOtherBtn, saveBtn, useBtn }) {
            b.setFont(Theme.ui(Font.PLAIN, 12));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        openOtherBtn.addActionListener(e -> {
            File start = new File("examples/.g4").getAbsoluteFile();
            if (!start.isDirectory()) start = new File(GRAMMAR_PATH).getAbsoluteFile().getParentFile();
            JFileChooser fc = new JFileChooser(start);
            fc.setDialogTitle("Elegir gramática ANTLR (.g4)");
            fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith(".g4"); }
                @Override public String getDescription() { return "Gramática ANTLR (*.g4)"; }
            });
            if (fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                current[0] = fc.getSelectedFile();
                loadGrammarInto(current[0], area, title);
            }
        });

        saveBtn.addActionListener(e -> {
            try {
                Files.write(current[0].toPath(), area.getText().getBytes(StandardCharsets.UTF_8));
                title.setText(current[0].getName() + "   ·   guardado");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog, "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        useBtn.addActionListener(e -> applyGrammarToProject(dialog, area,
            new JButton[]{ openOtherBtn, saveBtn, useBtn }));

        JPanel head = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        head.setBackground(Theme.LOADED.tint);
        head.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_GRID),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        head.add(openOtherBtn);
        head.add(saveBtn);
        head.add(useBtn);

        JPanel top = new JPanel(new BorderLayout());
        title.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));
        top.add(title, BorderLayout.NORTH);
        top.add(head, BorderLayout.SOUTH);

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);

        loadGrammarInto(current[0], area, title);
        dialog.setVisible(true);
    }

    /** Escribe la gramática del editor al proyecto, regenera el parser, recompila y reinicia. */
    private void applyGrammarToProject(JDialog dialog, JTextArea area, JButton[] buttons) {
        int ans = JOptionPane.showConfirmDialog(dialog,
            "Esto reemplazará grammar/Compiscript.g4, regenerará el parser ANTLR\n" +
            "y recompilará el proyecto. El IDE se reiniciará al terminar.\n\n¿Continuar?",
            "Usar en el proyecto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.OK_OPTION) return;

        for (JButton b : buttons) b.setEnabled(false);
        dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        consoleArea.setText("Aplicando gramática nueva y recompilando…\n");

        String content = area.getText();
        new SwingWorker<GrammarBuilder.Result, Void>() {
            @Override protected GrammarBuilder.Result doInBackground() {
                return GrammarBuilder.applyGrammar(content);
            }
            @Override protected void done() {
                GrammarBuilder.Result r;
                try {
                    r = get();
                } catch (InterruptedException | ExecutionException ex) {
                    dialog.setCursor(Cursor.getDefaultCursor());
                    for (JButton b : buttons) b.setEnabled(true);
                    JOptionPane.showMessageDialog(dialog, "Error inesperado: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                consoleArea.setText(r.log);
                consoleArea.setCaretPosition(0);
                if (r.ok) {
                    JOptionPane.showMessageDialog(dialog,
                        "Gramática aplicada y proyecto recompilado.\nEl IDE se reiniciará ahora.",
                        "Listo", JOptionPane.INFORMATION_MESSAGE);
                    GrammarBuilder.restartGui();
                } else {
                    dialog.setCursor(Cursor.getDefaultCursor());
                    for (JButton b : buttons) b.setEnabled(true);
                    if (r.log.split("\n", -1).length <= 14) {
                        JOptionPane.showMessageDialog(dialog, r.log.trim(),
                            "No se pudo aplicar la gramática", JOptionPane.WARNING_MESSAGE);
                    } else {
                        showTextDialog(dialog, "No se pudo aplicar la gramática", r.log);
                    }
                }
            }
        }.execute();
    }

    /** Diálogo modal con texto monoespaciado desplazable, para el registro de compilación. */
    private void showTextDialog(Window owner, String heading, String text) {
        JDialog d = new JDialog(owner, heading, Dialog.ModalityType.APPLICATION_MODAL);
        d.setSize(680, 480);
        d.setLocationRelativeTo(owner);
        JTextArea ta = new JTextArea(text);
        ta.setFont(Theme.mono(Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setCaretPosition(0);
        d.add(new JScrollPane(ta), BorderLayout.CENTER);
        d.setVisible(true);
    }

    /** Carga un .g4 en el editor; si falla, muestra el aviso en el propio texto. */
    private void loadGrammarInto(File f, JTextArea area, JLabel title) {
        try {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            area.setText(content);
            area.setCaretPosition(0);
            title.setText(f.getName() + "   ·   editable  ·   \"Usar en el proyecto\" regenera y recompila");
        } catch (IOException ex) {
            area.setText("// No se pudo leer " + f.getPath() + "\n// " + ex.getMessage());
            title.setText("(no encontrada)");
        }
    }

    private void styleSecondaryButton(JButton b) {
        b.setFont(Theme.ui(Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 28));
        b.setMaximumSize(new Dimension(140, 28));
    }

    private JPanel buildRunPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        runBtn.setAlignmentX(CENTER_ALIGNMENT);
        runBtn.setMaximumSize(new Dimension(160, 44));
        runBtn.addActionListener(e -> runPipeline());

        statePill.setAlignmentX(CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(runBtn);
        p.add(Box.createVerticalStrut(8));
        p.add(statePill);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void initTabs() {
        tabs.addTab("Editor",    buildEditorTab());
        tabs.addTab("Errores",   makePlaceholder("Compila para ver errores y advertencias."));
        tabs.addTab("Árbol",     makePlaceholder("Compila para ver el árbol sintáctico."));
        tabs.addTab("Símbolos",  makePlaceholder("Compila para ver la tabla de símbolos."));
        tabs.addTab("Código TAC", makePlaceholder("Compila para ver el código intermedio (TAC)."));
        tabs.addTab("Registros de activación", makePlaceholder("Compila para ver los registros de activación."));
    }

    // ── Consola ─────────────────────────────────────────────────────────────
    /** Consola de texto que repite el reporte impreso en la terminal. */
    private JPanel buildConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_GRID));

        consoleHead = new JPanel(new BorderLayout());
        consoleHead.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        JLabel title = new JLabel("Consola");
        title.setFont(Theme.ui(Font.BOLD, 11));
        title.setForeground(new Color(51, 65, 85));

        clearConsoleBtn.setFont(Theme.ui(Font.PLAIN, 10));
        clearConsoleBtn.setFocusPainted(false);
        clearConsoleBtn.setBorderPainted(false);
        clearConsoleBtn.setContentAreaFilled(false);
        clearConsoleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearConsoleBtn.setForeground(new Color(100, 116, 139));
        clearConsoleBtn.addActionListener(e -> consoleArea.setText(""));

        consoleHead.add(title, BorderLayout.WEST);
        consoleHead.add(clearConsoleBtn, BorderLayout.EAST);

        consoleArea.setFont(Theme.mono(Font.PLAIN, 11));
        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        consoleArea.setBackground(new Color(252, 252, 255));
        consoleArea.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));
        consoleArea.setText("Compiscript IDE — listo. Carga o escribe un .cps y presiona Compilar.");

        JScrollPane scroll = new JScrollPane(consoleArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(100, 140));
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(consoleHead, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** Muestra el reporte de la compilación en la consola de la GUI y en stdout. */
    private void printReport(PipelineResult r) {
        String stamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String header = "[" + stamp + "] Compilando…\n";
        String body = r.report();
        consoleArea.setText(header + body);
        consoleArea.setCaretPosition(0);
        System.out.println(header.trim());
        System.out.print(body);
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_GRID));
        JLabel lbl = new JLabel("Compiscript IDE   |   ANTLR 4  ◆  Análisis semántico  ◆  Tabla de símbolos");
        lbl.setFont(Theme.ui(Font.PLAIN, 10));
        lbl.setForeground(new Color(148, 163, 184));
        bar.add(lbl);
        return bar;
    }

    // ── Máquina de estados de color ─────────────────────────────────────────
    /** Recolorea la ventana según la fase del pipeline. */
    private void applyPhase(Theme t, String text) {
        currentPhase = t;
        statePill.set(text, t.accent, t.soft);
        getContentPane().setBackground(t.tint);
        topBar.setBackground(t.tint);
        statusBar.setBackground(t.tint);
        consoleHead.setBackground(t.tint);
        runBtn.setAccent(t.accent);
        revalidate();
        repaint();
    }

    // ── Editor ──────────────────────────────────────────────────────────────
    private JComponent buildEditorTab() {
        editorArea.setFont(Theme.mono(Font.PLAIN, 14));
        editorArea.setBackground(new Color(252, 252, 255));
        editorArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        editorArea.setEditable(true);
        editorArea.setLineWrap(false);

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(editorArea), BorderLayout.CENTER);
        return p;
    }

    private void loadIntoEditor(File f) {
        if (f == null) return;
        try {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            suppressDocEvents = true;
            editorArea.setText(content);
            editorArea.setCaretPosition(0);
            suppressDocEvents = false;
            tabs.setSelectedIndex(TAB_EDITOR);
            applyPhase(Theme.LOADED, "Sin compilar");
        } catch (IOException ex) {
            editorArea.setText("// Error al leer el archivo: " + ex.getMessage());
        }
    }

    private void newFile() {
        suppressDocEvents = true;
        editorArea.setText("");
        suppressDocEvents = false;
        editorArea.requestFocusInWindow();
        applyPhase(Theme.IDLE, Theme.IDLE.label);
    }

    private void saveAs() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar programa Compiscript");
        fc.setSelectedFile(new File("programa.cps"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(fc.getSelectedFile().toPath(),
                    editorArea.getText().getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(this,
                    "Guardado: " + fc.getSelectedFile().getName(),
                    "Guardado", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Ejecución del pipeline ──────────────────────────────────────────────
    private void runPipeline() {
        String code = editorArea.getText();
        if (code.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El editor está vacío.",
                "Sin código", JOptionPane.WARNING_MESSAGE);
            return;
        }

        runBtn.setEnabled(false);
        statePill.set("Compilando…", currentPhase.accent, currentPhase.soft);

        SwingWorker<PipelineResult, Void> worker = new SwingWorker<PipelineResult, Void>() {
            @Override
            protected PipelineResult doInBackground() {
                return new PipelineRunner().run(code);
            }

            @Override
            protected void done() {
                try {
                    PipelineResult r = get();
                    populateAll(r);
                    printReport(r);
                    if (r.error != null) {
                        applyPhase(Theme.SEMANTIC, "Error: " + r.error);
                    } else if (r.isOk()) {
                        String warn = r.warnings.isEmpty() ? ""
                            : " · " + r.warnings.size() + " advertencia" + (r.warnings.size() > 1 ? "s" : "");
                        applyPhase(Theme.OK, "Sin errores" + warn);
                    } else {
                        int n = r.lexicalErrors.size() + r.syntaxErrors.size() + r.semanticErrors.size();
                        applyPhase(Theme.forPhase(r.phase()), errorSummary(r, n));
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    applyPhase(Theme.SEMANTIC, "Error inesperado: " + ex.getMessage());
                } finally {
                    runBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /** Texto del chip de estado tras una compilación con errores. */
    private String errorSummary(PipelineResult r, int n) {
        String base = n + " error" + (n > 1 ? "es" : "");
        if (!"mixed".equals(r.phase())) return base;

        java.util.List<String> fases = new java.util.ArrayList<>();
        if (!r.lexicalErrors.isEmpty()) fases.add("léxico");
        if (!r.syntaxErrors.isEmpty()) fases.add("sintáctico");
        if (!r.semanticErrors.isEmpty()) fases.add("semántico");
        return base + " · " + String.join(" + ", fases);
    }

    // ── Población de tabs ───────────────────────────────────────────────────
    private void populateAll(PipelineResult res) {
        tabs.setComponentAt(TAB_ERRORS,  buildErrorsTab(res));
        tabs.setComponentAt(TAB_TREE,    buildTreeTab(res));
        tabs.setComponentAt(TAB_SYMBOLS, buildSymbolsTab(res));
        tabs.setComponentAt(TAB_TAC,     buildTextTab(res.tac, "Sin código intermedio: corrige los errores para generarlo."));
        tabs.setComponentAt(TAB_FRAMES,  buildTextTab(res.activationMarkdown, "Sin registros de activación: corrige los errores para generarlos."));

        boolean hasErrors = !res.lexicalErrors.isEmpty() || !res.syntaxErrors.isEmpty() || !res.semanticErrors.isEmpty();
        tabs.setForegroundAt(TAB_ERRORS, hasErrors ? Theme.forPhase(res.phase()).accent : Theme.OK.accent);
        tabs.revalidate();
        tabs.repaint();
    }

    // ── Tab Errores ─────────────────────────────────────────────────────────
    private JComponent buildErrorsTab(PipelineResult res) {
        boolean hasErrors = !res.lexicalErrors.isEmpty() || !res.syntaxErrors.isEmpty() || !res.semanticErrors.isEmpty();
        Theme resultTheme = hasErrors ? Theme.forPhase(res.phase()) : Theme.OK;

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        north.setOpaque(false);
        north.add(new Pill(hasErrors ? "CON ERRORES" : "SIN ERRORES", resultTheme.accent, resultTheme.soft));
        if (!res.warnings.isEmpty())
            north.add(new Pill(res.warnings.size() + " advertencia(s)", Theme.IDLE.accent, Theme.IDLE.soft));

        ScrollableBoxPanel body = new ScrollableBoxPanel();
        body.setBackground(Color.WHITE);

        addErrorSection(body, "Errores léxicos", res.lexicalErrors, Theme.LEXICAL);
        addErrorSection(body, "Errores sintácticos", res.syntaxErrors, Theme.SYNTAX);
        addErrorSection(body, "Errores semánticos", res.semanticErrors, Theme.SEMANTIC);
        addErrorSection(body, "Advertencias", res.warnings, Theme.IDLE);

        if (!hasErrors && res.warnings.isEmpty()) {
            JLabel ok = new JLabel("✓  Sin errores ni advertencias.");
            ok.setFont(Theme.ui(Font.PLAIN, 13));
            ok.setForeground(Theme.OK.accent);
            ok.setBorder(BorderFactory.createEmptyBorder(30, 16, 16, 16));
            body.add(ok);
        }

        JPanel p = new JPanel(new BorderLayout());
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(body), BorderLayout.CENTER);
        return p;
    }

    private void addErrorSection(JPanel container, String title, List<SemanticError> list, Theme t) {
        if (list.isEmpty()) return;

        JLabel header = new JLabel("  " + title + "  ·  " + list.size());
        header.setFont(Theme.ui(Font.BOLD, 11));
        header.setForeground(t.accent);
        header.setOpaque(true);
        header.setBackground(t.soft);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        container.add(header);
        container.add(Box.createVerticalStrut(4));

        for (SemanticError e : list) {
            JTextArea line = new JTextArea("  " + e.format());
            line.setFont(Theme.mono(Font.PLAIN, 12));
            line.setEditable(false);
            line.setLineWrap(true);
            line.setWrapStyleWord(true);
            line.setBackground(Color.WHITE);
            line.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, t.accent),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            // El alto real lo recalcula el layout cuando el componente ya tiene ancho.
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            container.add(line);
            container.add(Box.createVerticalStrut(4));
        }
        container.add(Box.createVerticalStrut(10));
    }

    // ── Tab Árbol ───────────────────────────────────────────────────────────
    private JComponent buildTreeTab(PipelineResult res) {
        if (res.treeDot == null || res.treeDot.isEmpty())
            return makePlaceholder("Sin árbol disponible (revisa errores léxicos/sintácticos).");

        if (!GraphvizUtil.isAvailable()) {
            JPanel p = new JPanel(new BorderLayout());
            p.add(headerLabel("Graphviz no encontrado — mostrando DOT crudo. " +
                "Instálalo (brew install graphviz) para ver el árbol renderizado."), BorderLayout.NORTH);
            p.add(makeScrollText(res.treeDot), BorderLayout.CENTER);
            return p;
        }

        try {
            File tmpPng = File.createTempFile("tree_", ".png");
            tmpPng.deleteOnExit();
            if (!viz.GraphvizUtil.renderPng(res.treeDot, tmpPng)) {
                return makePlaceholder("No se pudo renderizar el árbol con Graphviz.");
            }
            BufferedImage original = ImageIO_read(tmpPng);
            if (original == null) return makePlaceholder("No se pudo leer la imagen renderizada.");

            double[] zoom = {1.0};
            JLabel imgLbl = new JLabel();
            imgLbl.setHorizontalAlignment(SwingConstants.CENTER);
            JScrollPane scroll = new JScrollPane(imgLbl);

            JLabel zoomLbl = new JLabel("100%");
            zoomLbl.setFont(Theme.ui(Font.BOLD, 12));
            zoomLbl.setPreferredSize(new Dimension(50, 24));
            zoomLbl.setHorizontalAlignment(SwingConstants.CENTER);

            Runnable applyZoom = () -> {
                int w = (int) (original.getWidth() * zoom[0]);
                int h = (int) (original.getHeight() * zoom[0]);
                Image scaled = original.getScaledInstance(Math.max(w, 1), Math.max(h, 1), Image.SCALE_SMOOTH);
                imgLbl.setIcon(new ImageIcon(scaled));
                imgLbl.revalidate();
                zoomLbl.setText(String.format("%.0f%%", zoom[0] * 100));
            };

            JButton btnIn = new JButton("＋");
            JButton btnOut = new JButton("－");
            JButton btnReset = new JButton("100%");
            btnIn.addActionListener(e -> { zoom[0] = Math.min(zoom[0] + 0.2, 4.0); applyZoom.run(); });
            btnOut.addActionListener(e -> { zoom[0] = Math.max(zoom[0] - 0.2, 0.1); applyZoom.run(); });
            btnReset.addActionListener(e -> { zoom[0] = 1.0; applyZoom.run(); });
            applyZoom.run();

            JPanel zoomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            zoomBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_GRID));
            zoomBar.add(new JLabel("Zoom:"));
            zoomBar.add(btnOut);
            zoomBar.add(zoomLbl);
            zoomBar.add(btnIn);
            zoomBar.add(btnReset);

            JPanel wrap = new JPanel(new BorderLayout());
            wrap.add(zoomBar, BorderLayout.NORTH);
            wrap.add(scroll, BorderLayout.CENTER);
            return wrap;
        } catch (IOException ex) {
            return makePlaceholder("Error al renderizar el árbol: " + ex.getMessage());
        }
    }

    private BufferedImage ImageIO_read(File f) throws IOException {
        return javax.imageio.ImageIO.read(f);
    }

    // ── Tab Símbolos ────────────────────────────────────────────────────────
    private JComponent buildSymbolsTab(PipelineResult res) {
        if (res.allScopes == null || res.allScopes.isEmpty())
            return makePlaceholder("Sin tabla de símbolos (revisa errores léxicos/sintácticos).");

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Color.WHITE);

        for (Scope sc : res.allScopes) {
            String parent = sc.parent != null ? "#" + sc.parent.id : "—";
            container.add(headerLabel(sc.fullLabel() + "    padre: " + parent));

            if (sc.symbols.isEmpty()) {
                JLabel empty = new JLabel("  (vacío)");
                empty.setFont(Theme.ui(Font.PLAIN, 12));
                empty.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
                empty.setForeground(new Color(148, 163, 184));
                container.add(empty);
                continue;
            }

            String[] cols = {"nombre", "clase", "tipo", "línea", "col", "almacenamiento", "offset", "tamaño", "etiqueta", "nivel"};
            Object[][] data = new Object[sc.symbols.size()][cols.length];
            int i = 0;
            for (Symbol s : sc.symbols.values()) {
                data[i][0] = s.name;
                data[i][1] = s.kind;
                data[i][2] = s.type.toString();
                data[i][3] = s.line;
                data[i][4] = s.column;
                data[i][5] = s.storage.isEmpty() ? "—" : s.storage;
                data[i][6] = s.offset < 0 ? "—" : String.valueOf(s.offset);
                data[i][7] = s.size == 0 ? "—" : String.valueOf(s.size);
                data[i][8] = s.label.isEmpty() ? "—" : s.label;
                data[i][9] = s.storage.isEmpty() ? "—" : String.valueOf(s.nestLevel);
                i++;
            }
            JTable table = makeTable(data, cols);
            table.setPreferredScrollableViewportSize(new Dimension(900, Math.min(22 * (data.length + 1), 220)));
            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            container.add(sp);
        }

        JScrollPane outer = new JScrollPane(container);
        outer.getVerticalScrollBar().setUnitIncrement(16);
        return outer;
    }

    // ── Tabs de texto (TAC, registros de activación) ───────────────────────
    private JComponent buildTextTab(String text, String emptyMsg) {
        if (text == null || text.isEmpty()) return makePlaceholder(emptyMsg);
        return makeScrollText(text);
    }

    // ── Componentes de UI reutilizables ─────────────────────────────────────
    private JTable makeTable(Object[][] data, String[] cols) {
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(Theme.mono(Font.PLAIN, 11));
        table.setRowHeight(22);
        table.getTableHeader().setFont(Theme.ui(Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setGridColor(C_GRID);
        table.setShowGrid(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                return this;
            }
        });
        return table;
    }

    private JComponent makePlaceholder(String msg) {
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setFont(Theme.ui(Font.PLAIN, 13));
        lbl.setForeground(new Color(148, 163, 184));
        return lbl;
    }

    private JScrollPane makeScrollText(String text) {
        return new JScrollPane(makeTextArea(text));
    }

    private JTextArea makeTextArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(Theme.mono(Font.PLAIN, 12));
        area.setEditable(false);
        area.setBackground(new Color(252, 252, 255));
        area.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return area;
    }

    private JLabel headerLabel(String text) {
        JLabel lbl = new JLabel("  " + text);
        lbl.setFont(Theme.ui(Font.BOLD, 11));
        lbl.setForeground(new Color(51, 65, 85));
        lbl.setOpaque(true);
        lbl.setBackground(new Color(241, 245, 249));
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_GRID),
            BorderFactory.createEmptyBorder(5, 6, 5, 6)));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return lbl;
    }

    // ── Punto de entrada ────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
