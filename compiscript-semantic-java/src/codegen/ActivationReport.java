package codegen;

import semantic.ClassSymbol;

import java.util.List;
import java.util.Map;

/** Reporte en Markdown de registros de activación, disposición de objetos y área global. */
public final class ActivationReport {
    private ActivationReport() {}

    public static String toMarkdown(TacGenerator g) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Registros de activación y disposición de memoria\n\n");
        sb.append("Todos los tamaños y desplazamientos están en bytes (palabra = 4 bytes; boolean ocupa 1 pero se alinea a 4).\n\n");
        sb.append("Cabecera de cada registro de activación (offset respecto a `fp`):\n\n");
        sb.append("| offset | campo |\n|---|---|\n");
        sb.append("| 0 | dirección de retorno |\n| 4 | enlace de control (fp del llamador) |\n");
        sb.append("| 8 | enlace de acceso (marco que define a la función) |\n| 12 | valor de retorno |\n\n");
        sb.append("Área global (`G`): ").append(g.globalSize).append(" bytes\n\n");

        for (Frame f : g.frames) {
            sb.append("## `").append(f.label).append("` — nivel ").append(f.level).append("\n\n");
            sb.append("- parámetros: ").append(f.paramsSize).append(" B · locales: ").append(f.localsSize)
              .append(" B · temporales: ").append(f.maxTemps).append(" (").append(f.maxTemps * 4)
              .append(" B) · **marco total: ").append(f.frameSize()).append(" B**\n");
            sb.append("- cabecera: 0..").append(Frame.HEADER - 1).append(" · temporales desde fp+").append(f.tempsOffset()).append("\n\n");
            if (!f.slots.isEmpty()) {
                sb.append("| fp+ | nombre | clase | tamaño |\n|---|---|---|---|\n");
                for (Frame.Slot s : f.slots)
                    sb.append("| ").append(s.offset).append(" | `").append(s.name).append("` | ").append(s.kind)
                      .append(" | ").append(s.size).append(" |\n");
                sb.append("\n");
            }
        }

        List<ClassSymbol> classes = g.classList;
        if (!classes.isEmpty()) {
            sb.append("# Disposición de objetos\n\n");
            for (ClassSymbol c : classes) {
                sb.append("## `").append(c.name).append("` — ").append(c.instanceSize).append(" B");
                if (c.classType.parent != null) sb.append(" (hereda de `").append(c.classType.parent.name).append("`)");
                sb.append("\n\n");
                if (c.fieldOffsets.isEmpty()) { sb.append("(sin atributos)\n\n"); continue; }
                sb.append("| offset | atributo |\n|---|---|\n");
                for (Map.Entry<String, Integer> e : c.fieldOffsets.entrySet())
                    sb.append("| ").append(e.getValue()).append(" | `").append(e.getKey()).append("` |\n");
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
