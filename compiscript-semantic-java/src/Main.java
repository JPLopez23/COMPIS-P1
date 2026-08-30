import gui.MainWindow;
import gui.PipelineResult;
import gui.PipelineRunner;
import viz.GraphvizUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Punto de entrada de Compiscript: modo CLI (batch) o subcomando 'gui' (IDE Swing). */
public class Main {

    public static void main(String[] args) {
        if (args.length >= 1 && (args[0].equals("gui") || args[0].equals("--gui") || args[0].equals("-gui"))) {
            MainWindow.main(new String[0]);
            return;
        }

        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String inputFile = args[0];
        String outDir = "output";
        boolean writeDot = false;
        boolean writePng = false;
        boolean writeSymbols = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-out": outDir = args[++i]; break;
                case "-dot": writeDot = true; break;
                case "-png": writePng = true; break;
                case "-symbols": writeSymbols = true; break;
                default:
                    System.err.println("Argumento desconocido: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        try {
            String source = new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
            PipelineResult res = new PipelineRunner().run(source);

            System.out.print(res.report());

            String base = baseName(inputFile);
            Path outPath = Paths.get(outDir);
            if (writeDot || writePng || writeSymbols) Files.createDirectories(outPath);

            if (writeDot) {
                Path p = outPath.resolve(base + ".dot");
                Files.write(p, res.treeDot.getBytes(StandardCharsets.UTF_8));
                System.out.println("dot: " + p);
            }
            if (writePng) {
                Path p = outPath.resolve(base + ".png");
                boolean ok = GraphvizUtil.renderPng(res.treeDot, p.toFile());
                System.out.println(ok ? "png: " + p : "png: falló (¿Graphviz instalado?)");
            }
            if (writeSymbols) {
                Path p = outPath.resolve(base + ".symbols.md");
                Files.write(p, res.symbolsMarkdown.getBytes(StandardCharsets.UTF_8));
                System.out.println("symbols: " + p);
            }

            System.exit(res.isOk() ? 0 : 1);
        } catch (IOException ex) {
            System.err.println("Error leyendo '" + inputFile + "': " + ex.getMessage());
            System.exit(1);
        }
    }

    private static String baseName(String path) {
        String name = Paths.get(path).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    private static void printUsage() {
        System.err.println("Uso:");
        System.err.println("  java -cp out Main gui                                    # Lanza el IDE gráfico (Swing)");
        System.err.println("  java -cp out Main <archivo.cps> [-out carpeta] [-dot] [-png] [-symbols]");
        System.err.println();
        System.err.println("  -out       Carpeta de salida (default: output)");
        System.err.println("  -dot       Escribe el árbol sintáctico en formato DOT");
        System.err.println("  -png       Renderiza el árbol a PNG (requiere Graphviz)");
        System.err.println("  -symbols   Escribe la tabla de símbolos en Markdown");
    }
}
