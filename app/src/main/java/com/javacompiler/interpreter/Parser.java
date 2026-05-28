package com.javacompiler.interpreter;

import java.util.ArrayList;
import java.util.List;


public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }


    public ASTNode.Program parse() throws ParseException {
        int line = cur().line;
        skipModifiers();
        expect(TokenType.CLASS, "Expected 'class'");
        String className = expectIdent("Expected class name after 'class'");
        expect(TokenType.LBRACE, "Expected '{' after class name '" + className + "'");

        List<ASTNode.MethodDecl> methods = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            methods.add(parseMethod());
        }

        if (!check(TokenType.RBRACE))
            throw new ParseException(
                    "Missing '}' to close class '" + className + "'", cur().line);
        advance();

        if (!check(TokenType.EOF))
            throw new ParseException(
                    "Unexpected code after class body: '" + cur().value + "'", cur().line);

        return new ASTNode.Program(new ASTNode.ClassDecl(className, methods, line), line);
    }

    private ASTNode.MethodDecl parseMethod() throws ParseException {
        int line = cur().line;
        skipModifiers();            // public static
        skipReturnType();           // void / int / etc.
        String name = expectIdent("Expected method name");
        expect(TokenType.LPAREN, "Expected '(' after method name '" + name + "'");
        skipUntil(TokenType.RPAREN);
        expect(TokenType.RPAREN, "Expected ')'");
        ASTNode.Block body = parseBlock();
        return new ASTNode.MethodDecl(name, body, line);
    }

    private ASTNode.Block parseBlock() throws ParseException {
        int line = cur().line;
        expect(TokenType.LBRACE, "Expected '{'");
        List<ASTNode.Statement> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            stmts.add(parseStatement());
        }
        if (!check(TokenType.RBRACE))
            throw new ParseException("Missing '}' to close block", cur().line);
        advance();
        return new ASTNode.Block(stmts, line);
    }

    private ASTNode.Statement parseStatement() throws ParseException {
        int line = cur().line;

        if (check(TokenType.IF))       return parseIf();
        if (check(TokenType.WHILE))    return parseWhile();
        if (check(TokenType.FOR))      return parseFor();
        if (check(TokenType.RETURN))   return parseReturn();
        if (check(TokenType.BREAK))  {
            advance();
            expect(TokenType.SEMICOLON, "Expected ';' after 'break'");
            return new ASTNode.BreakStatement(line);
        }
        if (check(TokenType.CONTINUE)) {
            advance();
            expect(TokenType.SEMICOLON, "Expected ';' after 'continue'");
            return new ASTNode.ContinueStatement(line);
        }


        if (isTypeKeyword(cur().type)) return parseVarDecl(true);


        if (cur().type == TokenType.IDENTIFIER && cur().value.equals("System"))
            return parsePrint();

        ASTNode.Expression expr = parseExpr();
        expect(TokenType.SEMICOLON, "Expected ';' after statement");
        return new ASTNode.ExprStatement(expr, line);
    }

    private ASTNode.Statement parseVarDecl(boolean requireSemicolon) throws ParseException {
        int line    = cur().line;
        String type = cur().value;
        advance();

        boolean isArray = false;
        if (check(TokenType.LBRACKET)) {
            advance();
            expect(TokenType.RBRACKET, "Expected ']' after '['");
            isArray = true;
        }

        String name = expectIdent("Expected variable name after type '" + type + "'");

        ASTNode.Expression init = null;
        if (check(TokenType.ASSIGN)) {
            advance();
            init = (isArray && check(TokenType.LBRACE))
                    ? parseArrayInit(type)
                    : parseExpr();
        }

        if (requireSemicolon)
            expect(TokenType.SEMICOLON, "Expected ';' after variable declaration of '" + name + "'");

        return new ASTNode.VarDecl(type, isArray, name, init, line);
    }

    private ASTNode.Expression parseArrayInit(String elementType) throws ParseException {
        int line = cur().line;
        expect(TokenType.LBRACE, "Expected '{'");
        List<ASTNode.Expression> items = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            items.add(parseExpr());
            if (check(TokenType.COMMA)) advance();
        }
        expect(TokenType.RBRACE, "Expected '}' to close array initializer");
        return new ASTNode.ArrayCreation(elementType, null, items, line);
    }


    private ASTNode.Statement parsePrint() throws ParseException {
        int line = cur().line;
        advance();
        expect(TokenType.DOT, "Expected '.' after 'System'");
        String stream = expectIdent("Expected 'out' after 'System.'");
        if (!stream.equals("out"))
            throw new ParseException("Expected 'System.out', got 'System." + stream + "'", line);
        expect(TokenType.DOT, "Expected '.' after 'System.out'");
        String method = expectIdent("Expected 'println' or 'print'");

        boolean newline;
        if      (method.equals("println")) newline = true;
        else if (method.equals("print"))   newline = false;
        else throw new ParseException(
                    "Unknown output method 'System.out." + method +
                            "'. Use System.out.println() or System.out.print()", line);

        expect(TokenType.LPAREN, "Expected '(' after '" + method + "'");
        ASTNode.Expression value = check(TokenType.RPAREN) ? null : parseExpr();
        expect(TokenType.RPAREN, "Expected ')' after argument");
        expect(TokenType.SEMICOLON, "Expected ';' after print statement");

        return new ASTNode.PrintStatement(value, newline, line);
    }


    private ASTNode.Statement parseIf() throws ParseException {
        int line = cur().line;
        advance(); // if
        expect(TokenType.LPAREN, "Expected '(' after 'if'");
        ASTNode.Expression cond = parseExpr();
        expect(TokenType.RPAREN, "Expected ')' to close if condition");
        ASTNode.Block thenBlock = parseBlock();

        ASTNode.Block elseBlock = null;
        if (check(TokenType.ELSE)) {
            advance();
            if (check(TokenType.IF)) {
                ASTNode.Statement elseIf = parseIf();
                List<ASTNode.Statement> list = new ArrayList<>();
                list.add(elseIf);
                elseBlock = new ASTNode.Block(list, line);
            } else {
                elseBlock = parseBlock();
            }
        }
        return new ASTNode.IfStatement(cond, thenBlock, elseBlock, line);
    }


    private ASTNode.Statement parseWhile() throws ParseException {
        int line = cur().line;
        advance();
        expect(TokenType.LPAREN, "Expected '(' after 'while'");
        ASTNode.Expression cond = parseExpr();
        expect(TokenType.RPAREN, "Expected ')' to close while condition");
        return new ASTNode.WhileStatement(cond, parseBlock(), line);
    }

    private ASTNode.Statement parseFor() throws ParseException {
        int line = cur().line;
        advance();
        expect(TokenType.LPAREN, "Expected '(' after 'for'");

        ASTNode.Statement init = null;
        if (!check(TokenType.SEMICOLON)) {
            if (isTypeKeyword(cur().type)) {
                init = parseVarDecl(false);      // no semicolon — we consume it next
            } else {
                init = new ASTNode.ExprStatement(parseExpr(), cur().line);
            }
        }
        expect(TokenType.SEMICOLON, "Expected ';' after for-init");

        ASTNode.Expression cond = check(TokenType.SEMICOLON) ? null : parseExpr();
        expect(TokenType.SEMICOLON, "Expected ';' after for-condition");

        ASTNode.Expression update = check(TokenType.RPAREN) ? null : parseExpr();
        expect(TokenType.RPAREN, "Expected ')' to close for header");

        return new ASTNode.ForStatement(init, cond, update, parseBlock(), line);
    }

    private ASTNode.Statement parseReturn() throws ParseException {
        int line = cur().line;
        advance();
        ASTNode.Expression value = check(TokenType.SEMICOLON) ? null : parseExpr();
        expect(TokenType.SEMICOLON, "Expected ';' after return");
        return new ASTNode.ReturnStatement(value, line);
    }

    private ASTNode.Expression parseExpr()       throws ParseException { return parseAssign(); }

    private ASTNode.Expression parseAssign() throws ParseException {
        int line = cur().line;
        ASTNode.Expression left = parseOr();
        if (isAssignOp(cur().type)) {
            String op = cur().value;
            advance();
            return new ASTNode.AssignExpr(left, op, parseAssign(), line); // right-assoc
        }
        return left;
    }

    private ASTNode.Expression parseOr()  throws ParseException {
        int l = cur().line;
        ASTNode.Expression e = parseAnd();
        while (check(TokenType.OR_OR))  { advance(); e = new ASTNode.BinaryExpr(e, "||", parseAnd(), l); }
        return e;
    }
    private ASTNode.Expression parseAnd() throws ParseException {
        int l = cur().line;
        ASTNode.Expression e = parseEq();
        while (check(TokenType.AND_AND)) { advance(); e = new ASTNode.BinaryExpr(e, "&&", parseEq(), l); }
        return e;
    }
    private ASTNode.Expression parseEq() throws ParseException {
        int l = cur().line;
        ASTNode.Expression e = parseCmp();
        while (check(TokenType.EQUAL_EQUAL) || check(TokenType.NOT_EQUAL)) {
            String op = cur().value; advance();
            e = new ASTNode.BinaryExpr(e, op, parseCmp(), l);
        }
        return e;
    }
    private ASTNode.Expression parseCmp() throws ParseException {
        int l = cur().line;
        ASTNode.Expression e = parseAdd();
        while (check(TokenType.LESS) || check(TokenType.GREATER) ||
                check(TokenType.LESS_EQUAL) || check(TokenType.GREATER_EQUAL)) {
            String op = cur().value; advance();
            e = new ASTNode.BinaryExpr(e, op, parseAdd(), l);
        }
        return e;
    }
    private ASTNode.Expression parseAdd() throws ParseException {
        int l = cur().line;
        ASTNode.Expression e = parseMul();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = cur().value; advance();
            e = new ASTNode.BinaryExpr(e, op, parseMul(), l);
        }
        return e;
    }
    private ASTNode.Expression parseMul() throws ParseException {
        int l = cur().line;
        ASTNode.Expression e = parseUnary();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            String op = cur().value; advance();
            e = new ASTNode.BinaryExpr(e, op, parseUnary(), l);
        }
        return e;
    }

    private ASTNode.Expression parseUnary() throws ParseException {
        int l = cur().line;
        if (check(TokenType.BANG))       { advance(); return new ASTNode.UnaryExpr("!", parseUnary(), true, l); }
        if (check(TokenType.MINUS))      { advance(); return new ASTNode.UnaryExpr("-", parseUnary(), true, l); }
        if (check(TokenType.PLUS_PLUS))  { advance(); return new ASTNode.UnaryExpr("++", parsePostfix(), true, l); }
        if (check(TokenType.MINUS_MINUS)){ advance(); return new ASTNode.UnaryExpr("--", parsePostfix(), true, l); }
        return parsePostfix();
    }

    private ASTNode.Expression parsePostfix() throws ParseException {
        int l = cur().line;
        ASTNode.Expression e = parsePrimary();

        while (true) {
            if (check(TokenType.PLUS_PLUS)) {
                advance();
                e = new ASTNode.UnaryExpr("++", e, false, l);
            } else if (check(TokenType.MINUS_MINUS)) {
                advance();
                e = new ASTNode.UnaryExpr("--", e, false, l);
            } else if (check(TokenType.LBRACKET)) {
                advance();
                ASTNode.Expression idx = parseExpr();
                expect(TokenType.RBRACKET, "Expected ']' after index");
                e = new ASTNode.ArrayAccess(e, idx, l);
            } else if (check(TokenType.DOT)) {
                advance();
                String field = expectIdent("Expected field/method name after '.'");
                if (check(TokenType.LPAREN)) {
                    advance();
                    List<ASTNode.Expression> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        args.add(parseExpr());
                        while (check(TokenType.COMMA)) { advance(); args.add(parseExpr()); }
                    }
                    expect(TokenType.RPAREN, "Expected ')' after arguments");
                    e = new ASTNode.MethodCall(e, field, args, l);
                } else {
                    e = new ASTNode.FieldAccess(e, field, l);
                }
            } else {
                break;
            }
        }
        return e;
    }

    private ASTNode.Expression parsePrimary() throws ParseException {
        int l = cur().line;
        Token t = cur();

        switch (t.type) {
            case INTEGER_LITERAL:
                advance();
                return new ASTNode.IntLiteral(Integer.parseInt(t.value), l);
            case DOUBLE_LITERAL:
                advance();
                return new ASTNode.DoubleLiteral(Double.parseDouble(t.value), l);
            case STRING_LITERAL:
                advance();
                return new ASTNode.StringLiteral(t.value, l);
            case CHAR_LITERAL:
                advance();
                return new ASTNode.CharLiteral(t.value.charAt(0), l);
            case BOOLEAN_LITERAL:
                advance();
                return new ASTNode.BoolLiteral(t.value.equals("true"), l);
            case NULL_LITERAL:
                advance();
                return new ASTNode.NullLiteral(l);
            case LPAREN: {
                advance();
                ASTNode.Expression e = parseExpr();
                expect(TokenType.RPAREN, "Expected ')' to close grouped expression");
                return e;
            }
            case NEW:
                return parseNew();
            case IDENTIFIER: {
                advance();
                if (check(TokenType.LPAREN)) {
                    advance();
                    List<ASTNode.Expression> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        args.add(parseExpr());
                        while (check(TokenType.COMMA)) { advance(); args.add(parseExpr()); }
                    }
                    expect(TokenType.RPAREN, "Expected ')'");
                    return new ASTNode.MethodCall(null, t.value, args, l);
                }
                return new ASTNode.VarRef(t.value, l);
            }
            default:
                throw new ParseException(
                        "Unexpected token '" + t.value + "' — expected an expression", l);
        }
    }

    private ASTNode.Expression parseNew() throws ParseException {
        int l = cur().line;
        advance();
        if (!isTypeKeyword(cur().type) && cur().type != TokenType.IDENTIFIER)
            throw new ParseException("Expected type after 'new'", l);
        String elemType = cur().value;
        advance();
        if (!check(TokenType.LBRACKET))
            throw new ParseException("Expected '[' for array creation (e.g. new int[5])", l);
        advance();
        ASTNode.Expression size = parseExpr();
        expect(TokenType.RBRACKET, "Expected ']' after array size");
        return new ASTNode.ArrayCreation(elemType, size, null, l);
    }



    private boolean isTypeKeyword(TokenType t) {
        return t == TokenType.INT || t == TokenType.DOUBLE_TYPE || t == TokenType.STRING_TYPE
                || t == TokenType.BOOLEAN_TYPE || t == TokenType.CHAR_TYPE
                || t == TokenType.FLOAT_TYPE   || t == TokenType.LONG_TYPE;
    }

    private boolean isAssignOp(TokenType t) {
        return t == TokenType.ASSIGN       || t == TokenType.PLUS_ASSIGN
                || t == TokenType.MINUS_ASSIGN || t == TokenType.STAR_ASSIGN
                || t == TokenType.SLASH_ASSIGN || t == TokenType.PERCENT_ASSIGN;
    }

    private void skipModifiers() {
        while (check(TokenType.PUBLIC) || check(TokenType.PRIVATE) || check(TokenType.PROTECTED)
                || check(TokenType.STATIC) || check(TokenType.FINAL))
            advance();
    }

    private void skipReturnType() throws ParseException {
        if (check(TokenType.VOID) || isTypeKeyword(cur().type)) {
            advance();
            if (check(TokenType.LBRACKET)) { advance(); expect(TokenType.RBRACKET, "Expected ']'"); }
        }
    }

    private void skipUntil(TokenType stop) {
        int depth = 0;
        while (!check(TokenType.EOF)) {
            if (cur().type == stop && depth == 0) return;
            if (cur().type == TokenType.LPAREN)   depth++;
            if (cur().type == TokenType.RPAREN)   depth--;
            advance();
        }
    }

    private Token cur()                    { return tokens.get(pos); }
    private boolean check(TokenType t)    { return cur().type == t; }
    private Token advance()               { Token t = tokens.get(pos); if (pos < tokens.size()-1) pos++; return t; }

    private Token expect(TokenType t, String msg) throws ParseException {
        if (!check(t)) throw new ParseException(msg + " (found '" + cur().value + "')", cur().line);
        return advance();
    }

    private String expectIdent(String msg) throws ParseException {
        if (cur().type != TokenType.IDENTIFIER)
            throw new ParseException(msg + " (found '" + cur().value + "')", cur().line);
        return advance().value;
    }
}