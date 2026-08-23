package viz;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Renderiza texto DOT a imagen invocando el comando `dot` de Graphviz. */
public class GraphvizUtil {

    /** Genera un PNG en outFile a partir del DOT. Devuelve false si falla o si no hay Graphviz. */
    public static boolean renderPng(String dotSource, File outFile) {
        try {
            File tmpDot = File.createTempFile("tree_", ".dot");
            tmpDot.deleteOnExit();
            Files.write(tmpDot.toPath(), dotSource.getBytes(StandardCharsets.UTF_8));
            Process proc = Runtime.getRuntime().exec(
                new String[]{"dot", "-Tpng", tmpDot.getAbsolutePath(), "-o", outFile.getAbsolutePath()});
            proc.waitFor();
            return outFile.exists() && outFile.length() > 0;
        } catch (IOException | InterruptedException ex) {
            return false;
        }
    }

    /** Indica si el comando `dot` está disponible en el PATH. */
    public static boolean isAvailable() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"dot", "-V"});
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
