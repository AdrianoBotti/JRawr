// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionParseTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

public class ParseError {
    private String message;
    private int code;
    private int line;
    private int col;
    private int pos;
    private int length;

    public int getCode() {
        return code;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return col;
    }

    public int getPosition() {
        return pos;
    }

    public int getLength() {
        return length;
    }

    public String getMessage() {
        return message;
    }

    public ParseError(String message, int code, ParseNode node) {
        this(message, code, 0, node.token.getStartPos(), node.token.getStartPos(), node.token.getLength());
    }

    public ParseError(String message, int code, int line, int col, int pos, int length) {
        this.message = message;
        this.code = code;
        this.line = line;
        this.col = col;
        this.pos = pos;
        this.length = length;
    }
}
