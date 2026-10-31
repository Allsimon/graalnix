package org.graalnix.parser;

import static org.graalnix.parser.TokenType.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.graalnix.parser.Ast.*;

/** Recursive-descent port of Nix's grammar (src/libexpr/parser.y). */
public final class Parser {
    private final Lexer lexer;
    private Token cur;
    private Token prev;

    private Parser(String src) {
        this.lexer = new Lexer(src);
        this.cur = lexer.next();
    }

    public static Expr parse(String src) {
        Parser p = new Parser(src);
        Expr e = p.parseExpr();
        if (p.cur.type() != EOF) throw p.unexpected();
        return e;
    }

    private Token advance() {
        prev = cur;
        cur = lexer.next();
        return prev;
    }

    private boolean at(TokenType t) {
        return cur.type() == t;
    }

    private boolean accept(TokenType t) {
        if (cur.type() != t) return false;
        advance();
        return true;
    }

    private Token expect(TokenType t) {
        if (cur.type() != t) throw unexpected();
        return advance();
    }

    private ParseException unexpected() {
        String what = cur.type() == EOF ? "end of file" : "'" + cur.text() + "'";
        return new ParseException("syntax error, unexpected " + what, cur.start());
    }

    private Pos from(int start) {
        return new Pos(start, prev != null ? prev.end() : start);
    }

    /** Looks ahead {@code n} tokens past {@code cur} without consuming anything. */
    private TokenType peek(int n) {
        int saved = lexer.pos;
        try {
            Token t = cur;
            for (int i = 0; i < n; i++) t = lexer.next();
            return t.type();
        } catch (ParseException e) {
            return EOF;
        } finally {
            lexer.pos = saved;
        }
    }

    private Expr parseExpr() {
        int start = cur.start();
        switch (cur.type()) {
            case ID -> {
                TokenType next = peek(1);
                if (next == COLON) {
                    String arg = advance().text();
                    advance();
                    Expr body = parseExpr();
                    return new Lambda(from(start), arg, null, body);
                }
                if (next == AT) {
                    String arg = advance().text();
                    advance();
                    expect(LBRACE);
                    return parseFormalsLambda(start, arg);
                }
            }
            case LBRACE -> {
                if (isFormals()) {
                    advance();
                    return parseFormalsLambda(start, null);
                }
            }
            case ASSERT -> {
                advance();
                Expr cond = parseExpr();
                expect(SEMI);
                Expr body = parseExpr();
                return new Assert(from(start), cond, body);
            }
            case WITH -> {
                advance();
                Expr scope = parseExpr();
                expect(SEMI);
                Expr body = parseExpr();
                return new With(from(start), scope, body);
            }
            case LET -> {
                if (peek(1) != LBRACE) {
                    advance();
                    Attrs binds = new Attrs(new Pos(start, start), true);
                    parseBinds(binds, IN);
                    expect(IN);
                    for (DynAttr d : binds.dynamic) {
                        throw new ParseException(
                                "dynamic attributes not allowed in let", d.pos().start());
                    }
                    Expr body = parseExpr();
                    return new Let(from(start), binds, body);
                }
            }
            default -> {}
        }
        return parseIf();
    }

    private boolean isFormals() {
        TokenType t1 = peek(1);
        if (t1 == ELLIPSIS) return true;
        if (t1 == RBRACE) {
            TokenType t2 = peek(2);
            return t2 == COLON || t2 == AT;
        }
        if (t1 == ID) {
            TokenType t2 = peek(2);
            if (t2 == COMMA || t2 == QUESTION) return true;
            if (t2 == RBRACE) {
                TokenType t3 = peek(3);
                return t3 == COLON || t3 == AT;
            }
        }
        return false;
    }

    /** Parses formals after the opening brace, then the optional {@code @name}, the colon and the body. */
    private Expr parseFormalsLambda(int start, String arg) {
        List<Formal> formals = new ArrayList<>();
        Set<String> names = new HashSet<>();
        boolean ellipsis = false;
        while (!at(RBRACE)) {
            if (accept(ELLIPSIS)) {
                ellipsis = true;
                break;
            }
            Token name = expect(ID);
            Expr fallback = accept(QUESTION) ? parseExpr() : null;
            if (!names.add(name.text())) {
                throw new ParseException("duplicate formal function argument '" + name.text() + "'", name.start());
            }
            formals.add(new Formal(new Pos(name.start(), prev.end()), name.text(), fallback));
            if (!accept(COMMA)) break;
        }
        expect(RBRACE);
        if (arg == null && accept(AT)) arg = expect(ID).text();
        if (arg != null && names.contains(arg)) {
            throw new ParseException("duplicate formal function argument '" + arg + "'", start);
        }
        expect(COLON);
        Expr body = parseExpr();
        return new Lambda(from(start), arg, new Formals(formals, ellipsis), body);
    }

    private Expr parseIf() {
        if (at(IF)) {
            int start = cur.start();
            advance();
            Expr cond = parseExpr();
            expect(THEN);
            Expr then = parseExpr();
            expect(ELSE);
            Expr otherwise = parseExpr();
            return new If(from(start), cond, then, otherwise);
        }
        return parseBinary(1);
    }

    private static final int PREC_NOT = 7;
    private static final int PREC_NEGATE = 12;

    private static int infixPrec(TokenType t) {
        return switch (t) {
            case IMPL -> 1;
            case OR -> 2;
            case AND -> 3;
            case EQ, NEQ -> 4;
            case LT, GT, LEQ, GEQ -> 5;
            case UPDATE -> 6;
            case PLUS, MINUS -> 8;
            case STAR, SLASH -> 9;
            case CONCAT -> 10;
            case QUESTION -> 11;
            default -> -1;
        };
    }

    private Expr parseBinary(int minPrec) {
        int start = cur.start();
        Expr left = parseUnary();
        while (true) {
            TokenType t = cur.type();
            int prec = infixPrec(t);
            if (prec < minPrec || prec < 0) return left;
            advance();
            if (t == QUESTION) {
                left = new HasAttr(from(start), left, parseAttrPath());
                continue;
            }
            boolean rightAssoc = t == IMPL || t == UPDATE || t == CONCAT;
            Expr right = parseBinary(rightAssoc ? prec : prec + 1);
            Pos pos = from(start);
            left = switch (t) {
                case IMPL -> new BinOp(pos, Op.IMPL, left, right);
                case OR -> new BinOp(pos, Op.OR, left, right);
                case AND -> new BinOp(pos, Op.AND, left, right);
                case EQ -> new BinOp(pos, Op.EQ, left, right);
                case NEQ -> new BinOp(pos, Op.NEQ, left, right);
                case LT -> new BinOp(pos, Op.LT, left, right);
                case GT -> new BinOp(pos, Op.GT, left, right);
                case LEQ -> new BinOp(pos, Op.LE, left, right);
                case GEQ -> new BinOp(pos, Op.GE, left, right);
                case UPDATE -> new BinOp(pos, Op.UPDATE, left, right);
                case PLUS -> new BinOp(pos, Op.ADD, left, right);
                case MINUS -> new BinOp(pos, Op.SUB, left, right);
                case STAR -> new BinOp(pos, Op.MUL, left, right);
                case SLASH -> new BinOp(pos, Op.DIV, left, right);
                case CONCAT -> new BinOp(pos, Op.CONCAT, left, right);
                default -> throw new IllegalStateException();
            };
            if ((prec == 4 || prec == 5) && infixPrec(cur.type()) == prec) throw unexpected();
        }
    }

    private Expr parseUnary() {
        int start = cur.start();
        if (accept(NOT)) {
            Expr operand = parseBinary(PREC_NOT + 1);
            return new Not(from(start), operand);
        }
        if (accept(MINUS)) {
            Expr operand = parseBinary(PREC_NEGATE + 1);
            Pos pos = from(start);
            return new BinOp(pos, Op.SUB, new IntLit(pos, 0), operand);
        }
        return parseApp();
    }

    private boolean startsSimple() {
        return switch (cur.type()) {
            case ID, INT, FLOAT, PATH, HPATH, SPATH, URI, STR_OPEN, IND_STR_OPEN, LPAREN, LBRACK, LBRACE, REC -> true;
            case LET -> peek(1) == LBRACE;
            default -> false;
        };
    }

    private Expr parseApp() {
        int start = cur.start();
        Expr fn = parseSelect();
        if (!startsSimple()) return fn;
        List<Expr> args = new ArrayList<>();
        while (startsSimple()) args.add(parseSelect());
        return new Call(from(start), fn, args);
    }

    private Expr parseSelect() {
        int start = cur.start();
        Expr e = parseSimple();
        if (accept(DOT)) {
            List<AttrName> path = parseAttrPath();
            Expr fallback = accept(OR_KW) ? parseSelect() : null;
            return new Select(from(start), e, path, fallback);
        }
        if (at(OR_KW)) {
            Token or = advance();
            return new Call(from(start), e, List.of(new Var(new Pos(or.start(), or.end()), "or")));
        }
        return e;
    }

    private Expr parseSimple() {
        int start = cur.start();
        Token t = cur;
        switch (t.type()) {
            case ID -> {
                advance();
                return t.text().equals("__curPos") ? new CurPos(from(start)) : new Var(from(start), t.text());
            }
            case INT -> {
                advance();
                try {
                    return new IntLit(from(start), Long.parseLong(t.text()));
                } catch (NumberFormatException e) {
                    throw new ParseException("invalid integer '" + t.text() + "'", start);
                }
            }
            case FLOAT -> {
                advance();
                return new FloatLit(from(start), Double.parseDouble(t.text()));
            }
            case STR_OPEN -> {
                return parseString(start);
            }
            case IND_STR_OPEN -> {
                return parseIndString(start);
            }
            case PATH, HPATH -> {
                return parsePath(start);
            }
            case SPATH -> {
                advance();
                return new SearchPath(
                        from(start), t.text().substring(1, t.text().length() - 1));
            }
            case URI -> {
                advance();
                return new StrLit(from(start), t.text());
            }
            case LPAREN -> {
                advance();
                Expr e = parseExpr();
                expect(RPAREN);
                return e;
            }
            case LET -> {
                advance();
                expect(LBRACE);
                Attrs binds = new Attrs(new Pos(start, start), true);
                parseBinds(binds, RBRACE);
                expect(RBRACE);
                Pos pos = from(start);
                return new Select(pos, binds, List.of(new AttrName("body", null)), null);
            }
            case REC, LBRACE -> {
                boolean rec = accept(REC);
                expect(LBRACE);
                Attrs attrs = new Attrs(new Pos(start, start), rec);
                parseBinds(attrs, RBRACE);
                expect(RBRACE);
                attrs.pos = from(start);
                return attrs;
            }
            case LBRACK -> {
                advance();
                List<Expr> items = new ArrayList<>();
                while (!at(RBRACK)) items.add(parseSelect());
                advance();
                return new ListExpr(from(start), items);
            }
            default -> throw unexpected();
        }
    }

    private Expr parseString(int start) {
        if (!at(STR_OPEN)) throw unexpected();
        prev = cur;
        List<Expr> parts = new ArrayList<>();
        boolean interpolated = false;
        while (true) {
            int partStart = lexer.pos;
            Lexer.Part p = lexer.stringPart();
            if (p.kind() == Lexer.PartKind.END) break;
            if (p.kind() == Lexer.PartKind.LIT) {
                parts.add(new StrLit(new Pos(partStart, lexer.pos), p.text()));
            } else {
                parts.add(parseInterpolation());
                interpolated = true;
            }
        }
        cur = lexer.next();
        prev = new Token(STR_OPEN, "\"", start, cur.start(), false);
        Pos pos = from(start);
        if (parts.isEmpty()) return new StrLit(pos, "");
        if (!interpolated) return new StrLit(pos, ((StrLit) parts.getFirst()).value());
        return new Interp(pos, false, parts);
    }

    /** Called right after {@code ${}; leaves the lexer right after the closing brace. */
    private Expr parseInterpolation() {
        cur = lexer.next();
        Expr e = parseExpr();
        if (!at(RBRACE)) throw unexpected();
        return e;
    }

    private Expr parseIndString(int start) {
        prev = cur;
        List<Object> parts = new ArrayList<>();
        while (true) {
            Lexer.Part p = lexer.indStringPart();
            if (p.kind() == Lexer.PartKind.END) break;
            parts.add(p.kind() == Lexer.PartKind.LIT ? p : parseInterpolation());
        }
        cur = lexer.next();
        prev = new Token(IND_STR_OPEN, "''", start, cur.start(), false);
        return stripIndentation(from(start), parts);
    }

    /** Port of {@code ParserState::stripIndentation}. */
    private static Expr stripIndentation(Pos pos, List<Object> parts) {
        if (parts.isEmpty()) return new StrLit(pos, "");
        boolean atStartOfLine = true;
        int minIndent = 1000000;
        int curIndent = 0;
        for (Object o : parts) {
            if (!(o instanceof Lexer.Part p) || !p.hasIndentation()) {
                if (atStartOfLine) {
                    atStartOfLine = false;
                    if (curIndent < minIndent) minIndent = curIndent;
                }
                continue;
            }
            String s = p.text();
            for (int j = 0; j < s.length(); j++) {
                char c = s.charAt(j);
                if (atStartOfLine) {
                    if (c == ' ') {
                        curIndent++;
                    } else if (c == '\n') {
                        curIndent = 0;
                    } else {
                        atStartOfLine = false;
                        if (curIndent < minIndent) minIndent = curIndent;
                    }
                } else if (c == '\n') {
                    atStartOfLine = true;
                    curIndent = 0;
                }
            }
        }

        List<Expr> out = new ArrayList<>();
        atStartOfLine = true;
        int curDropped = 0;
        int n = parts.size();
        for (Object o : parts) {
            if (o instanceof Lexer.Part p) {
                String s = p.text();
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (atStartOfLine) {
                        if (c == ' ') {
                            if (curDropped++ >= minIndent) sb.append(c);
                        } else if (c == '\n') {
                            curDropped = 0;
                            sb.append(c);
                        } else {
                            atStartOfLine = false;
                            curDropped = 0;
                            sb.append(c);
                        }
                    } else {
                        sb.append(c);
                        if (c == '\n') atStartOfLine = true;
                    }
                }
                String s2 = sb.toString();
                if (n == 1) {
                    int nl = s2.lastIndexOf('\n');
                    if (nl >= 0 && s2.substring(nl + 1).chars().allMatch(ch -> ch == ' ')) s2 = s2.substring(0, nl + 1);
                }
                out.add(new StrLit(pos, s2));
            } else {
                atStartOfLine = false;
                curDropped = 0;
                out.add((Expr) o);
            }
            n--;
        }

        List<Expr> merged = new ArrayList<>();
        for (Expr e : out) {
            if (e instanceof StrLit s && !merged.isEmpty() && merged.getLast() instanceof StrLit l) {
                merged.set(merged.size() - 1, new StrLit(pos, l.value() + s.value()));
            } else {
                merged.add(e);
            }
        }
        if (merged.size() == 1 && merged.getFirst() instanceof StrLit s) return new StrLit(pos, s.value());
        return new Interp(pos, false, merged);
    }

    private Expr parsePath(int start) {
        Token t = cur;
        prev = t;
        String text = t.text();
        if (!t.interp()) {
            cur = lexer.next();
            return new PathLit(from(start), text);
        }
        List<Expr> parts = new ArrayList<>();
        parts.add(new PathLit(new Pos(t.start(), t.end()), text));
        String last = text;
        while (true) {
            int partStart = lexer.pos;
            Lexer.Part p = lexer.pathPart();
            if (p.kind() == Lexer.PartKind.END) break;
            if (p.kind() == Lexer.PartKind.LIT) {
                parts.add(new StrLit(new Pos(partStart, lexer.pos), p.text()));
                last = p.text();
            } else {
                parts.add(parseInterpolation());
                last = "";
            }
        }
        if (last.endsWith("/")) throw new ParseException("path has a trailing slash", lexer.pos);
        cur = lexer.next();
        prev = new Token(PATH, "", start, lexer.pos, false);
        return new Interp(new Pos(start, prev.end()), true, parts);
    }

    private AttrName parseAttr() {
        int start = cur.start();
        switch (cur.type()) {
            case ID, OR_KW -> {
                return new AttrName(advance().text(), null);
            }
            case STR_OPEN -> {
                Expr s = parseString(start);
                return s instanceof StrLit l ? new AttrName(l.value(), null) : new AttrName(null, s);
            }
            case DOLLAR_CURLY -> {
                advance();
                Expr e = parseExpr();
                expect(RBRACE);
                return e instanceof StrLit l ? new AttrName(l.value(), null) : new AttrName(null, e);
            }
            default -> throw unexpected();
        }
    }

    private List<AttrName> parseAttrPath() {
        List<AttrName> path = new ArrayList<>();
        path.add(parseAttr());
        while (accept(DOT)) path.add(parseAttr());
        return path;
    }

    private void parseBinds(Attrs attrs, TokenType end) {
        while (!at(end)) {
            int start = cur.start();
            if (accept(INHERIT)) {
                InheritSource source = null;
                if (accept(LPAREN)) {
                    source = new InheritSource(parseExpr());
                    expect(RPAREN);
                }
                while (!at(SEMI)) {
                    int nameStart = cur.start();
                    AttrName name = parseAttr();
                    if (name.dynamic() != null) {
                        throw new ParseException("dynamic attributes not allowed in inherit", nameStart);
                    }
                    Pos pos = from(nameStart);
                    if (attrs.attrs.containsKey(name.name())) throw dupAttr(name.name(), nameStart);
                    attrs.attrs.put(
                            name.name(),
                            source == null
                                    ? new AttrDef(new Var(pos, name.name()), pos, AttrKind.INHERITED, null)
                                    : new AttrDef(null, pos, AttrKind.INHERITED_FROM, source));
                }
                advance();
            } else {
                List<AttrName> path = parseAttrPath();
                expect(ASSIGN);
                Expr value = parseExpr();
                expect(SEMI);
                addAttr(attrs, path, value, from(start));
            }
        }
    }

    private static ParseException dupAttr(String path, int offset) {
        return new ParseException("attribute '" + path + "' already defined", offset);
    }

    private static String showPath(List<AttrName> path) {
        return String.join(".", path.stream().map(AttrName::toString).toList());
    }

    /** Port of {@code ParserState::addAttr}, which merges nested attribute paths. */
    private static void addAttr(Attrs attrs, List<AttrName> path, Expr value, Pos pos) {
        for (int i = 0; i < path.size() - 1; i++) {
            AttrName name = path.get(i);
            Attrs nested;
            if (name.name() != null) {
                AttrDef existing = attrs.attrs.get(name.name());
                if (existing != null) {
                    if (existing.kind() != AttrKind.INHERITED && existing.value() instanceof Attrs a) {
                        nested = a;
                    } else {
                        throw dupAttr(showPath(path.subList(0, i + 1)), pos.start());
                    }
                } else {
                    nested = new Attrs(pos, false);
                    attrs.attrs.put(name.name(), new AttrDef(nested, pos, AttrKind.PLAIN, null));
                }
            } else {
                nested = new Attrs(pos, false);
                attrs.dynamic.add(new DynAttr(name.dynamic(), nested, pos));
            }
            attrs = nested;
        }
        AttrName last = path.getLast();
        if (last.name() == null) {
            attrs.dynamic.add(new DynAttr(last.dynamic(), value, pos));
            return;
        }
        AttrDef existing = attrs.attrs.get(last.name());
        if (existing == null) {
            attrs.attrs.put(last.name(), new AttrDef(value, pos, AttrKind.PLAIN, null));
            return;
        }
        if (existing.kind() != AttrKind.INHERITED
                && existing.value() instanceof Attrs target
                && value instanceof Attrs ae) {
            for (var e : ae.attrs.entrySet()) {
                if (target.attrs.containsKey(e.getKey()))
                    throw dupAttr(e.getKey(), e.getValue().pos().start());
                target.attrs.put(e.getKey(), e.getValue());
            }
            target.dynamic.addAll(ae.dynamic);
        } else {
            throw dupAttr(showPath(path), pos.start());
        }
    }
}
