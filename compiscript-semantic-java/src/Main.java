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
        boolean writeTac = false;
        boolean showTac = false;
        boolean writeMips = false;
        boolean showMips = false;
        boolean runMips = false;
        boolean showRegs = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-out": outDir = args[++i]; break;
                case "-dot": writeDot = true; break;
                case "-png": writePng = true; break;
                case "-symbols": writeSymbols = true; break;
                case "-tac": writeTac = true; break;
                case "-print-tac": showTac = true; break;
                case "-mips": writeMips = true; break;
                case "-print-mips": showMips = true; break;
                case "-run": runMips = true; break;
                case "-regs": showRegs = true; break;
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
            if (writeDot || writePng || writeSymbols || writeTac || writeMips) Files.createDirectories(outPath);

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

            if (showTac && !res.tac.isEmpty()) System.out.print(res.tac);
            if (writeTac) {
                if (res.tac.isEmpty()) {
                    System.out.println("tac: no generado (hay errores)");
                } else {
                    Path p = outPath.resolve(base + ".tac");
                    Files.write(p, res.tac.getBytes(StandardCharsets.UTF_8));
                    System.out.println("tac: " + p);
                    Path ar = outPath.resolve(base + ".frames.md");
                    Files.write(ar, res.activationMarkdown.getBytes(StandardCharsets.UTF_8));
                    System.out.println("registros de activación: " + ar);
                }
            }

            if (showRegs && !res.registerReport.isEmpty()) System.out.print(res.registerReport);
            if (showMips && !res.mips.isEmpty()) System.out.print(res.mips);
            if (writeMips) {
                if (res.mips.isEmpty()) {
                    System.out.println("mips: no generado (hay errores)");
                } else {
                    Path p = outPath.resolve(base + ".asm");
                    Files.write(p, res.mips.getBytes(StandardCharsets.UTF_8));
                    System.out.println("mips: " + p);
                }
            }
            if (runMips) {
                if (res.mips.isEmpty()) {
                    System.out.println("run: no hay código MIPS para ejecutar (hay errores)");
                } else {
                    mips.MipsSimulator.Result r = mips.MipsSimulator.run(res.mips);
                    System.out.println("--- salida del programa (simulador MIPS) ---");
                    System.out.print(r.output);
                    if (r.error != null) System.out.println("--- error de ejecución: " + r.error);
                    else System.out.println("--- fin (" + r.steps + " instrucciones) ---");
                }
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
        System.err.println("  java -cp out Main <archivo.cps> [-out carpeta] [-dot] [-png] [-symbols] [-tac] [-print-tac] [-mips] [-print-mips] [-run] [-regs]");
        System.err.println();
        System.err.println("  -out       Carpeta de salida (default: output)");
        System.err.println("  -dot       Escribe el árbol sintáctico en formato DOT");
        System.err.println("  -png       Renderiza el árbol a PNG (requiere Graphviz)");
        System.err.println("  -symbols   Escribe la tabla de símbolos (extendida) en Markdown");
        System.err.println("  -tac       Escribe el código intermedio (.tac) y los registros de activación (.frames.md)");
        System.err.println("  -print-tac Imprime el código intermedio por consola");
        System.err.println("  -mips      Escribe el ensamblador MIPS (.asm)");
        System.err.println("  -print-mips Imprime el ensamblador MIPS por consola");
        System.err.println("  -regs      Muestra el uso de registros $s y los derrames a la pila por función");
        System.err.println("  -run       Ejecuta el MIPS generado en el simulador incluido");
    }
}
