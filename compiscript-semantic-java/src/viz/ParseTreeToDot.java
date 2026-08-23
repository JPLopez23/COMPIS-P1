package viz;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.Map;

/** Convierte el ParseTree de ANTLR a texto DOT para Graphviz. */
public class ParseTreeToDot {

    private int nodeId = 0;
    private final Map<ParseTree, Integer> nodeIds = new HashMap<>();
    private StringBuilder sb;
    private String[] ruleNames;

    /** Devuelve el grafo DOT del árbol con raíz en root. */
    public String toDot(ParseTree root, Parser parser, String graphName) {
        nodeId = 0;
        nodeIds.clear();
        sb = new StringBuilder();
        ruleNames = parser.getRuleNames();

        sb.append("digraph ").append(sanitizeId(graphName)).append(" {\n");
        sb.append("    node [shape=box, fontname=\"Consolas\", fontsize=10];\n");
        sb.append("    edge [fontname=\"Consolas\", fontsize=9];\n\n");

        if (root != null) emitNode(root);

        sb.append("}\n");
        return sb.toString();
    }

    private int getId(ParseTree n) {
        return nodeIds.computeIfAbsent(n, k -> ++nodeId);
    }

    /** Escribe el nodo y, recursivamente, sus hijos y aristas. */
    private void emitNode(ParseTree n) {
        int id = getId(n);
        String label = label(n);

        if (n instanceof TerminalNode) {
            sb.append("    n").append(id).append(" [label=\"").append(escape(label))
              .append("\", style=filled, fillcolor=\"#EEF2FF\"];\n");
        } else {
            sb.append("    n").append(id).append(" [label=\"").append(escape(label)).append("\"];\n");
            for (int i = 0; i < n.getChildCount(); i++) {
                ParseTree child = n.getChild(i);
                emitNode(child);
                sb.append("    n").append(id).append(" -> n").append(getId(child)).append(";\n");
            }
        }
    }

    /** Texto del token para las hojas, nombre de la regla para los nodos internos. */
    private String label(ParseTree n) {
        if (n instanceof TerminalNode) return n.getText();
        if (n instanceof ParserRuleContext) {
            int idx = ((ParserRuleContext) n).getRuleIndex();
            if (idx >= 0 && idx < ruleNames.length) return ruleNames[idx];
        }
        return n.getClass().getSimpleName();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String sanitizeId(String s) {
        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
