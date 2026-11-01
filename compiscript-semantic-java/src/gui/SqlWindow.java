package gui;

import semantic.SemanticError;
import sql.SqlPipeline;
import sql.ast.Ast;
import sql.exec.ExecResult;
import sql.storage.Catalog;
import sql.storage.Database;
import sql.storage.TableSchema;
import sql.storage.Values;
import viz.GraphvizUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * IDE gráfico del intérprete SQL: editor, tokens, árbol sintáctico, AST, errores por fase,
 * ejecución y resultados, y el estado de la base de datos en archivos.
 */
public class SqlWindow extends JFrame {

    private static final Color C_GRID = new Color(226, 232, 240);

    private final JTextArea editor = new JTextArea();
    private final JLabel dbLabel = new JLabel();
    private final RoundedButton runBtn = new RoundedButton("Ejecutar", Theme.IDLE.accent);
    private final Pill statePill = new Pill(Theme.IDLE.label, Theme.IDLE.accent, Theme.IDLE.soft);
    private final JTabbedPane tabs = new JTabbedPane();
    private JPanel topBar;

    private static final int TAB_RESULTS = 0, TAB_TOKENS = 1, TAB_TREE = 2, TAB_AST = 3, TAB_ERRORS = 4, TAB_DB = 5;

    private Database db;
    private Path dbDir;
    private SqlPipeline.Result lastResult;
    private final javax.swing.Timer liveTimer;
    private boolean suppressEvents;

    public SqlWindow(Path dbDir) {
        super("Intérprete SQL con almacenamiento en archivos");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1180, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        openDatabase(dbDir);

        add(topBar = buildTopBar(), BorderLayout.NORTH);

        editor.setFont(Theme.mono(Font.PLAIN, 14));
        editor.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        editor.setTabSize(2);
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setBorder(BorderFactory.createTitledBorder("Sentencias SQL (separadas por ;)"));

        tabs.setFont(Theme.ui(Font.PLAIN, 12));
        tabs.addTab("Resultados", placeholder("Ejecuta las sentencias para ver los resultados."));
        tabs.addTab("Tokens", placeholder("Analiza para ver los tokens."));
        tabs.addTab("Árbol sintáctico", placeholder("Analiza para ver el árbol sintáctico."));
        tabs.addTab("AST", placeholder("Analiza para ver el AST."));
        tabs.addTab("Errores", placeholder("Analiza para ver los errores."));
        tabs.addTab("Base de datos", placeholder(""));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScroll, tabs);
        split.setResizeWeight(0.3);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        suppressEvents = true;
        editor.setText(
            "-- Escribe sentencias SQL aquí. Ejemplo:\n"
            + "CREATE TABLE estudiantes (id INT PRIMARY KEY, nombre VARCHAR(50) NOT NULL, edad INT);\n"
            + "INSERT INTO estudiantes VALUES (1, 'Ana', 20), (2, 'Luis', 17);\n"
            + "SELECT nombre FROM estudiantes WHERE edad >= 18;\n");
        suppressEvents = false;

        // el análisis (sin ejecutar) se actualiza solo poco después de dejar de escribir
        liveTimer = new javax.swing.Timer(500, e -> analyze());
        liveTimer.setRepeats(false);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { edited(); }
            @Override public void removeUpdate(DocumentEvent e) { edited(); }
            @Override public void changedUpdate(DocumentEvent e) { edited(); }
        });

        refreshDatabaseTab();
        apply(Theme.IDLE, Theme.IDLE.label);
        analyze();
    }

    private void edited() {
        if (suppressEvents) return;
        liveTimer.restart();
    }

    private void openDatabase(Path dir) {
        try {
            this.db = new Database(dir);
            this.dbDir = dir;
            dbLabel.setText("Base de datos: " + dir.toAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir la base de datos:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Barra superior ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_GRID),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JButton open = secondary("Abrir .sql");
        JButton save = secondary("Guardar como…");
        JButton choose = secondary("Carpeta de BD…");
        JButton analyze = secondary("Solo analizar");
        JButton compiscript = secondary("Ir a Compiscript");
        open.addActionListener(e -> openFile());
        save.addActionListener(e -> saveFile());
        choose.addActionListener(e -> chooseDatabase());
        analyze.addActionListener(e -> analyze());
        compiscript.addActionListener(e -> { dispose(); MainWindow.main(new String[0]); });
        runBtn.addActionListener(e -> execute());

        dbLabel.setFont(Theme.ui(Font.PLAIN, 12));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        for (JButton b : new JButton[]{open, save, choose, analyze, compiscript}) buttons.add(b);
        left.add(buttons);
        left.add(Box.createVerticalStrut(6));
        JPanel dbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dbRow.setOpaque(false);
        dbRow.add(dbLabel);
        left.add(dbRow);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        runBtn.setAlignmentX(CENTER_ALIGNMENT);
        statePill.setAlignmentX(CENTER_ALIGNMENT);
        right.add(runBtn);
        right.add(Box.createVerticalStrut(6));
        right.add(statePill);

        bar.add(left);
        bar.add(Box.createHorizontalGlue());
        bar.add(right);
        return bar;
    }

    private JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.ui(Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void apply(Theme t, String text) {
        statePill.set(text, t.accent, t.soft);
        getContentPane().setBackground(t.tint);
        if (topBar != null) topBar.setBackground(t.tint);
        runBtn.setAccent(t.accent);
        revalidate();
        repaint();
    }

    // ── Archivos ────────────────────────────────────────────────────────────
    private void openFile() {
        JFileChooser fc = new JFileChooser(new File("."));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String text = new String(Files.readAllBytes(fc.getSelectedFile().toPath()), StandardCharsets.UTF_8);
            editor.setText(text);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el archivo:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile() {
        JFileChooser fc = new JFileChooser(new File("."));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.write(fc.getSelectedFile().toPath(), editor.getText().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chooseDatabase() {
        JFileChooser fc = new JFileChooser(dbDir.toAbsolutePath().getParent() == null ? new File(".") : dbDir.toAbsolutePath().getParent().toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Carpeta de la base de datos (se crea si no existe)");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        openDatabase(fc.getSelectedFile().toPath());
        refreshDatabaseTab();
        analyze();
    }

    // ── Pipeline ────────────────────────────────────────────────────────────
    private void analyze() {
        liveTimer.stop();
        run(false);
    }

    private void execute() {
        liveTimer.stop();
        run(true);
    }

    private void run(boolean execute) {
        // el catálogo se vuelve a leer del disco por si los archivos cambiaron fuera de la aplicación
        openDatabase(dbDir);
        SqlPipeline.Result res = new SqlPipeline().run(editor.getText(), db, execute);
        lastResult = res;
        show(res, execute);
    }

    private void show(SqlPipeline.Result res, boolean executed) {
        tabs.setComponentAt(TAB_RESULTS, resultsPanel(res, executed));
        tabs.setComponentAt(TAB_TOKENS, tokensPanel(res));
        tabs.setComponentAt(TAB_TREE, treePanel(res));
        tabs.setComponentAt(TAB_AST, astPanel(res));
        tabs.setComponentAt(TAB_ERRORS, errorsPanel(res));
        int errors = res.lexicalErrors.size() + res.syntaxErrors.size() + res.semanticErrors.size() + res.executionErrors.size();
        tabs.setTitleAt(TAB_ERRORS, errors == 0 ? "Errores" : "Errores (" + errors + ")");
        refreshDatabaseTab();

        Theme t = Theme.forPhase(res.phase());
        String label = t.label;
        if (res.isOk()) label = executed ? "Ejecutado" : "Sin errores";
        apply(t, label);
        if (executed) tabs.setSelectedIndex(res.isOk() ? TAB_RESULTS : TAB_ERRORS);
    }

    // ── Pestañas ────────────────────────────────────────────────────────────
    private JComponent placeholder(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.ui(Font.ITALIC, 13));
        l.setForeground(Color.GRAY);
        return l;
    }

    private JTable table(String[] columns, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] r : rows) model.addRow(r);
        JTable t = new JTable(model);
        t.setFont(Theme.mono(Font.PLAIN, 13));
        t.setRowHeight(22);
        t.setGridColor(C_GRID);
        t.getTableHeader().setFont(Theme.ui(Font.BOLD, 12));
        return t;
    }

    private JComponent resultsPanel(SqlPipeline.Result res, boolean executed) {
        if (!executed) return placeholder("Pulsa «Ejecutar» para correr las sentencias y ver los resultados.");
        if (res.results.isEmpty()) return placeholder(res.isOk() ? "No hay sentencias." : "No se ejecutó nada: revisa la pestaña Errores.");
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        int n = 1;
        for (ExecResult r : res.results) {
            JLabel title = new JLabel(n++ + ". " + r.statement + (r.isQuery ? "  ·  " + r.rows.size() + (r.rows.size() == 1 ? " fila" : " filas") : ""));
            title.setFont(Theme.ui(Font.BOLD, 13));
            title.setAlignmentX(LEFT_ALIGNMENT);
            box.add(title);
            box.add(Box.createVerticalStrut(4));
            if (r.isQuery) {
                List<Object[]> rows = new ArrayList<>();
                for (Object[] row : r.rows) {
                    Object[] cells = new Object[row.length];
                    for (int i = 0; i < row.length; i++) cells[i] = Values.display(row[i]);
                    rows.add(cells);
                }
                JTable t = table(r.columns.toArray(new String[0]), rows);
                JScrollPane sp = new JScrollPane(t);
                sp.setAlignmentX(LEFT_ALIGNMENT);
                int h = Math.min(260, 28 + 22 * Math.max(rows.size(), 1));
                sp.setPreferredSize(new Dimension(400, h));
                sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
                box.add(sp);
            } else {
                JLabel msg = new JLabel(r.message);
                msg.setFont(Theme.ui(Font.PLAIN, 13));
                msg.setAlignmentX(LEFT_ALIGNMENT);
                box.add(msg);
            }
            for (String note : r.notes) {
                JLabel nl = new JLabel("   · " + note);
                nl.setFont(Theme.ui(Font.ITALIC, 12));
                nl.setForeground(new Color(0, 112, 80));
                nl.setAlignmentX(LEFT_ALIGNMENT);
                box.add(nl);
            }
            JLabel read = new JLabel("   · filas leídas de los archivos: " + r.rowsRead);
            read.setFont(Theme.ui(Font.PLAIN, 11));
            read.setForeground(Color.GRAY);
            read.setAlignmentX(LEFT_ALIGNMENT);
            box.add(read);
            box.add(Box.createVerticalStrut(14));
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(box, BorderLayout.NORTH);
        return new JScrollPane(wrap);
    }

    private JComponent tokensPanel(SqlPipeline.Result res) {
        if (res.tokens.isEmpty()) return placeholder("Sin tokens.");
        List<Object[]> rows = new ArrayList<>();
        int i = 1;
        for (SqlPipeline.TokenInfo t : res.tokens) rows.add(new Object[]{i++, t.type, t.text, t.line, t.col});
        return new JScrollPane(table(new String[]{"#", "Token", "Texto", "Línea", "Columna"}, rows));
    }

    private JComponent treePanel(SqlPipeline.Result res) {
        if (res.treeText.isEmpty()) return placeholder("Sin árbol sintáctico.");
        JTextArea area = new JTextArea(res.treeText);
        area.setEditable(false);
        area.setFont(Theme.mono(Font.PLAIN, 12));
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        JButton img = secondary(GraphvizUtil.isAvailable() ? "Ver como imagen (Graphviz)" : "Graphviz no está instalado");
        img.setEnabled(GraphvizUtil.isAvailable());
        img.addActionListener(e -> showTreeImage(res.treeDot));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(img);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void showTreeImage(String dot) {
        try {
            File png = File.createTempFile("sqltree_", ".png");
            png.deleteOnExit();
            if (!GraphvizUtil.renderPng(dot, png)) {
                JOptionPane.showMessageDialog(this, "Graphviz no pudo generar la imagen.");
                return;
            }
            JDialog d = new JDialog(this, "Árbol sintáctico", false);
            d.add(new JScrollPane(new JLabel(new ImageIcon(ImageIO.read(png)))));
            d.setSize(900, 650);
            d.setLocationRelativeTo(this);
            d.setVisible(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo mostrar la imagen: " + e.getMessage());
        }
    }

    private JComponent astPanel(SqlPipeline.Result res) {
        if (res.ast == null) return placeholder("Sin AST: corrige los errores léxicos y sintácticos.");
        DefaultMutableTreeNode root = build(res.ast);
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setFont(Theme.mono(Font.PLAIN, 13));
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        return new JScrollPane(tree);
    }

    private DefaultMutableTreeNode build(Ast.Node n) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(n.label());
        for (Ast.Node c : n.children()) node.add(build(c));
        return node;
    }

    private JComponent errorsPanel(SqlPipeline.Result res) {
        List<Object[]> rows = new ArrayList<>();
        add(rows, "Léxico", res.lexicalErrors);
        add(rows, "Sintáctico", res.syntaxErrors);
        add(rows, "Semántico", res.semanticErrors);
        add(rows, "Ejecución", res.executionErrors);
        if (res.error != null) rows.add(new Object[]{"Interno", "", "", "", res.error});
        if (rows.isEmpty()) return placeholder("Sin errores léxicos, sintácticos ni semánticos.");
        JTable t = table(new String[]{"Fase", "Línea", "Columna", "Tipo", "Mensaje"}, rows);
        t.getColumnModel().getColumn(4).setPreferredWidth(700);
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() < 2 || t.getSelectedRow() < 0) return;
                Object line = t.getValueAt(t.getSelectedRow(), 1), col = t.getValueAt(t.getSelectedRow(), 2);
                if (line instanceof Integer && col instanceof Integer) goTo((Integer) line, (Integer) col);
            }
        });
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        JLabel hint = new JLabel("  Doble clic en un error para ir a su posición en el editor.");
        hint.setFont(Theme.ui(Font.ITALIC, 11));
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private void add(List<Object[]> rows, String phase, List<SemanticError> errors) {
        for (SemanticError e : errors) rows.add(new Object[]{phase, e.line, e.column, e.category, e.message});
    }

    private void goTo(int line, int col) {
        try {
            int offset = editor.getLineStartOffset(Math.max(0, line - 1)) + Math.max(0, col - 1);
            editor.requestFocusInWindow();
            editor.setCaretPosition(Math.min(offset, editor.getDocument().getLength()));
        } catch (javax.swing.text.BadLocationException ignored) { }
    }

    /** Muestra el esquema (tablas, columnas, índices) y los archivos de la carpeta de la base. */
    private void refreshDatabaseTab() {
        StringBuilder sb = new StringBuilder();
        Catalog cat = db.catalog();
        sb.append("Carpeta: ").append(dbDir.toAbsolutePath()).append("\n\n");
        if (cat.tables().isEmpty()) sb.append("(no hay tablas)\n");
        for (TableSchema t : cat.tables()) {
            sb.append("TABLA ").append(t.name).append('\n');
            for (TableSchema.Column c : t.columns)
                sb.append("  ").append(c.name).append("  ").append(c.typeText())
                    .append(c.primaryKey ? "  PRIMARY KEY" : c.notNull ? "  NOT NULL" : "").append('\n');
            for (Catalog.IndexDef i : cat.indexesOf(t.name))
                sb.append("  índice ").append(i.name).append(" sobre (").append(i.column).append(")\n");
            sb.append('\n');
        }
        sb.append("Archivos:\n");
        try (Stream<Path> files = Files.walk(dbDir)) {
            files.filter(Files::isRegularFile).sorted().forEach(f -> {
                try {
                    sb.append("  ").append(dbDir.relativize(f)).append("  (").append(Files.size(f)).append(" bytes)\n");
                } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(Theme.mono(Font.PLAIN, 12));
        tabs.setComponentAt(TAB_DB, new JScrollPane(area));
    }

    public static void main(String[] args) {
        String dir = "database";
        for (int i = 0; i < args.length; i++) if (args[i].equals("-db") && i + 1 < args.length) dir = args[++i];
        final String chosen = dir;
        SwingUtilities.invokeLater(() -> new SqlWindow(Paths.get(chosen)).setVisible(true));
    }
}
