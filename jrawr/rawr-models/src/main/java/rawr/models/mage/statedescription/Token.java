// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionScanner.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

public class Token {
    private int startpos;
    private int endpos;
    private String text;
    private Object value;

    public int getStartPos() {
        return startpos;
    }

    public void setStartPos(int value) {
        startpos = value;
    }

    public int getLength() {
        return endpos - startpos;
    }

    public int getEndPos() {
        return endpos;
    }

    public void setEndPos(int value) {
        endpos = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String value) {
        text = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public TokenType type;

    public Token() {
        this(0, 0);
    }

    public Token(int start, int end) {
        type = TokenType._UNDETERMINED_;
        startpos = start;
        endpos = end;
        text = ""; // must initialize with empty string, may cause null reference exceptions otherwise
        value = null;
    }

    public void updateRange(Token token) {
        if (token.getStartPos() < startpos) startpos = token.getStartPos();
        if (token.getEndPos() > endpos) endpos = token.getEndPos();
    }

    @Override
    public String toString() {
        if (text != null)
            return type.toString() + " '" + text + "'";
        else
            return type.toString();
    }
}
