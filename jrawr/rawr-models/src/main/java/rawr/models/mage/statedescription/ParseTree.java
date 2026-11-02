// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionParseTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

import java.util.ArrayList;
import java.util.List;

public class ParseTree extends ParseNode {
    public ParseErrors errors;

    public List<Token> skipped;

    public ParseTree() {
        super(new Token(), "ParseTree");
        token.type = TokenType.Start;
        token.setText("Root");
        skipped = new ArrayList<Token>();
        errors = new ParseErrors();
    }

    public String printTree() {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        printNode(sb, this, indent);
        return sb.toString();
    }

    private void printNode(StringBuilder sb, ParseNode node, int indent) {
        String space = " ".repeat(indent);
        sb.append(space);
        sb.append(node.getText()).append('\n');
        for (ParseNode n : node.nodes) {
            printNode(sb, n, indent + 2);
        }
    }

    // this is the entry point for executing and evaluating the parse tree
    public StateDescriptionDelegate compile() {
        return nodes.get(0).compile(this);
    }

    public boolean interpret(int effects) {
        return nodes.get(0).interpret(this, effects);
    }
}
