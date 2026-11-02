// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionScanner.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scanner {
    public String input;
    public int startPos = 0;
    public int endPos = 0;
    public int currentLine;
    public int currentColumn;
    public int currentPosition;
    public List<Token> skipped; // tokens that were skipped
    public Map<TokenType, Pattern> patterns;

    private Token lookAheadToken;
    private List<TokenType> tokens;
    private List<TokenType> skipList; // tokens to be skipped

    public Scanner() {
        patterns = new LinkedHashMap<TokenType, Pattern>();
        tokens = new ArrayList<TokenType>();
        lookAheadToken = null;

        skipList = new ArrayList<TokenType>();

        patterns.put(TokenType.STATE, Pattern.compile("[^\\(\\)!\\|\\+-]+"));
        tokens.add(TokenType.STATE);

        patterns.put(TokenType.INTERSECTION, Pattern.compile("\\|"));
        tokens.add(TokenType.INTERSECTION);

        patterns.put(TokenType.UNION, Pattern.compile("\\+"));
        tokens.add(TokenType.UNION);

        patterns.put(TokenType.DIFFERENCE, Pattern.compile("-"));
        tokens.add(TokenType.DIFFERENCE);

        patterns.put(TokenType.COMPLEMENT, Pattern.compile("!"));
        tokens.add(TokenType.COMPLEMENT);

        patterns.put(TokenType.BROPEN, Pattern.compile("\\("));
        tokens.add(TokenType.BROPEN);

        patterns.put(TokenType.BRCLOSE, Pattern.compile("\\)"));
        tokens.add(TokenType.BRCLOSE);

        patterns.put(TokenType.EOF, Pattern.compile("^$"));
        tokens.add(TokenType.EOF);
    }

    public void init(String input) {
        this.input = input;
        startPos = 0;
        endPos = 0;
        currentLine = 0;
        currentColumn = 0;
        currentPosition = 0;
        skipped = new ArrayList<Token>();
        lookAheadToken = null;
    }

    public Token getToken(TokenType type) {
        Token t = new Token(this.startPos, this.endPos);
        t.type = type;
        return t;
    }

    // executes a lookahead of the next token and will advance the scan on the input string
    public Token scan(TokenType... expectedtokens) {
        Token tok = lookAhead(expectedtokens); // temporarely retrieve the lookahead
        lookAheadToken = null; // reset lookahead token, so scanning will continue
        startPos = tok.getEndPos();
        endPos = tok.getEndPos(); // set the tokenizer to the new scan position
        return tok;
    }

    // returns token with longest best match
    public Token lookAhead(TokenType... expectedtokens) {
        int i;
        int startpos = startPos;
        Token tok = null;
        List<TokenType> scantokens;

        // this prevents double scanning and matching
        // increased performance
        if (lookAheadToken != null
                && lookAheadToken.type != TokenType._UNDETERMINED_
                && lookAheadToken.type != TokenType._NONE_) return lookAheadToken;

        // if no scantokens specified, then scan for all of them (= backward compatible)
        if (expectedtokens.length == 0)
            scantokens = tokens;
        else {
            scantokens = new ArrayList<TokenType>();
            for (TokenType t : expectedtokens) scantokens.add(t);
            scantokens.addAll(skipList);
        }

        do {

            int len = -1;
            int indexValue = Integer.MAX_VALUE;
            TokenType index = null;
            String input = this.input.substring(startpos);

            tok = new Token(startpos, endPos);

            for (i = 0; i < scantokens.size(); i++) {
                Pattern r = patterns.get(scantokens.get(i));
                Matcher m = r.matcher(input);
                if (m.lookingAt() && ((m.end() > len) || (scantokens.get(i).ordinal() < indexValue && m.end() == len))) {
                    len = m.end();
                    indexValue = scantokens.get(i).ordinal();
                    index = scantokens.get(i);
                }
            }

            if (index != null && len >= 0) {
                tok.setEndPos(startpos + len);
                tok.setText(input.substring(tok.getStartPos(), tok.getStartPos() + len));
                tok.type = index;
            } else {
                if (tok.getStartPos() < tok.getEndPos() - 1)
                    tok.setText(input.substring(tok.getStartPos(), tok.getStartPos() + 1));
            }

            if (skipList.contains(tok.type)) {
                startpos = tok.getEndPos();
                skipped.add(tok);
            }
        }
        while (skipList.contains(tok.type));

        lookAheadToken = tok;
        return tok;
    }
}
