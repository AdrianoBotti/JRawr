// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionParser.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

public class Parser {
    private Scanner scanner;
    private ParseTree tree;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    public ParseTree parse(String input) {
        tree = new ParseTree();
        return parse(input, tree);
    }

    public ParseTree parse(String input, ParseTree tree) {
        scanner.init(input);

        this.tree = tree;
        parseStart(tree);
        tree.skipped = scanner.skipped;

        return tree;
    }

    private void parseStart(ParseNode parent) {
        Token tok;
        ParseNode n;
        ParseNode node = parent.createNode(scanner.getToken(TokenType.Start), "Start");
        parent.nodes.add(node);

        tok = scanner.lookAhead(TokenType.COMPLEMENT, TokenType.STATE, TokenType.BROPEN);
        if (tok.type == TokenType.COMPLEMENT
                || tok.type == TokenType.STATE
                || tok.type == TokenType.BROPEN) {
            parseUnionExpr(node);
        }

        tok = scanner.scan(TokenType.EOF);
        if (tok.type != TokenType.EOF)
            tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.EOF.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
        n = node.createNode(tok, tok.toString());
        node.token.updateRange(tok);
        node.nodes.add(n);

        parent.token.updateRange(node.token);
    }

    private void parseUnionExpr(ParseNode parent) {
        Token tok;
        ParseNode n;
        ParseNode node = parent.createNode(scanner.getToken(TokenType.UnionExpr), "UnionExpr");
        parent.nodes.add(node);

        parseDiffExpr(node);

        tok = scanner.lookAhead(TokenType.UNION);
        while (tok.type == TokenType.UNION) {

            tok = scanner.scan(TokenType.UNION);
            if (tok.type != TokenType.UNION)
                tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.UNION.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
            n = node.createNode(tok, tok.toString());
            node.token.updateRange(tok);
            node.nodes.add(n);

            parseDiffExpr(node);
            tok = scanner.lookAhead(TokenType.UNION);
        }

        parent.token.updateRange(node.token);
    }

    private void parseDiffExpr(ParseNode parent) {
        Token tok;
        ParseNode n;
        ParseNode node = parent.createNode(scanner.getToken(TokenType.DiffExpr), "DiffExpr");
        parent.nodes.add(node);

        parseIntExpr(node);

        tok = scanner.lookAhead(TokenType.DIFFERENCE);
        if (tok.type == TokenType.DIFFERENCE) {

            tok = scanner.scan(TokenType.DIFFERENCE);
            if (tok.type != TokenType.DIFFERENCE)
                tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.DIFFERENCE.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
            n = node.createNode(tok, tok.toString());
            node.token.updateRange(tok);
            node.nodes.add(n);

            parseIntExpr(node);
        }

        parent.token.updateRange(node.token);
    }

    private void parseIntExpr(ParseNode parent) {
        Token tok;
        ParseNode n;
        ParseNode node = parent.createNode(scanner.getToken(TokenType.IntExpr), "IntExpr");
        parent.nodes.add(node);

        parseCompExpr(node);

        tok = scanner.lookAhead(TokenType.INTERSECTION);
        while (tok.type == TokenType.INTERSECTION) {

            tok = scanner.scan(TokenType.INTERSECTION);
            if (tok.type != TokenType.INTERSECTION)
                tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.INTERSECTION.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
            n = node.createNode(tok, tok.toString());
            node.token.updateRange(tok);
            node.nodes.add(n);

            parseCompExpr(node);
            tok = scanner.lookAhead(TokenType.INTERSECTION);
        }

        parent.token.updateRange(node.token);
    }

    private void parseCompExpr(ParseNode parent) {
        Token tok;
        ParseNode n;
        ParseNode node = parent.createNode(scanner.getToken(TokenType.CompExpr), "CompExpr");
        parent.nodes.add(node);

        tok = scanner.lookAhead(TokenType.COMPLEMENT);
        if (tok.type == TokenType.COMPLEMENT) {
            tok = scanner.scan(TokenType.COMPLEMENT);
            if (tok.type != TokenType.COMPLEMENT)
                tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.COMPLEMENT.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
            n = node.createNode(tok, tok.toString());
            node.token.updateRange(tok);
            node.nodes.add(n);
        }

        parseAtom(node);

        parent.token.updateRange(node.token);
    }

    private void parseAtom(ParseNode parent) {
        Token tok;
        ParseNode n;
        ParseNode node = parent.createNode(scanner.getToken(TokenType.Atom), "Atom");
        parent.nodes.add(node);

        tok = scanner.lookAhead(TokenType.STATE, TokenType.BROPEN);
        switch (tok.type) {
            case STATE:
                tok = scanner.scan(TokenType.STATE);
                if (tok.type != TokenType.STATE)
                    tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.STATE.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
                n = node.createNode(tok, tok.toString());
                node.token.updateRange(tok);
                node.nodes.add(n);
                break;
            case BROPEN:

                tok = scanner.scan(TokenType.BROPEN);
                if (tok.type != TokenType.BROPEN)
                    tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.BROPEN.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
                n = node.createNode(tok, tok.toString());
                node.token.updateRange(tok);
                node.nodes.add(n);

                parseUnionExpr(node);

                tok = scanner.scan(TokenType.BRCLOSE);
                if (tok.type != TokenType.BRCLOSE)
                    tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found. Expected " + TokenType.BRCLOSE.toString(), 0x1001, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
                n = node.createNode(tok, tok.toString());
                node.token.updateRange(tok);
                node.nodes.add(n);
                break;
            default:
                tree.errors.add(new ParseError("Unexpected token '" + tok.getText().replace("\n", "") + "' found.", 0x0002, 0, tok.getStartPos(), tok.getStartPos(), tok.getLength()));
                break;
        }

        parent.token.updateRange(node.token);
    }
}
