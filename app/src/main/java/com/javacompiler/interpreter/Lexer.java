package com.javacompiler.interpreter;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final String source;
    private int pos  = 0;
    private int line = 1;
    private final List<Token> tokens = new ArrayList<>();

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() throws LexerException {
        while (pos < source.length()) {
            skipWhitespaceAndComments();
            if (pos >= source.length()) break;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);

            if (c == '\n')                       { line++; pos++; }
            else if (Character.isWhitespace(c))  { pos++; }
            else if (peek(0) == '/' && peek(1) == '/') {
                while (pos < source.length() && source.charAt(pos) != '\n') pos++;
            }
            else if (peek(0) == '/' && peek(1) == '*') {
                pos += 2;
                while (pos + 1 < source.length()) {
                    if (source.charAt(pos) == '\n') line++;
                    if (source.charAt(pos) == '*' && source.charAt(pos + 1) == '/') {
                        pos += 2;
                        break;
                    }
                    pos++;
                }
            }
            else break;
        }
    }

    private void scanToken() throws LexerException {
        char c = source.charAt(pos);

        if (c == '"')                              { scanString();     return; }
        if (c == '\'')                             { scanChar();       return; }
        if (Character.isDigit(c))                  { scanNumber();     return; }
        if (Character.isLetter(c) || c == '_')     { scanIdentifier(); return; }

        switch (c) {
            case '+':
                if (peek(1) == '+')      { emit(TokenType.PLUS_PLUS,      "++"); pos += 2; }
                else if (peek(1) == '=') { emit(TokenType.PLUS_ASSIGN,    "+="); pos += 2; }
                else                     { emit(TokenType.PLUS,            "+"); pos++;    }
                break;
            case '-':
                if (peek(1) == '-')      { emit(TokenType.MINUS_MINUS,    "--"); pos += 2; }
                else if (peek(1) == '=') { emit(TokenType.MINUS_ASSIGN,   "-="); pos += 2; }
                else                     { emit(TokenType.MINUS,           "-"); pos++;    }
                break;
            case '*':
                if (peek(1) == '=')      { emit(TokenType.STAR_ASSIGN,    "*="); pos += 2; }
                else                     { emit(TokenType.STAR,            "*"); pos++;    }
                break;
            case '/':
                if (peek(1) == '=')      { emit(TokenType.SLASH_ASSIGN,   "/="); pos += 2; }
                else                     { emit(TokenType.SLASH,           "/"); pos++;    }
                break;
            case '%':
                if (peek(1) == '=')      { emit(TokenType.PERCENT_ASSIGN, "%="); pos += 2; }
                else                     { emit(TokenType.PERCENT,         "%"); pos++;    }
                break;
            case '=':
                if (peek(1) == '=')      { emit(TokenType.EQUAL_EQUAL,   "=="); pos += 2; }
                else                     { emit(TokenType.ASSIGN,          "="); pos++;    }
                break;
            case '!':
                if (peek(1) == '=')      { emit(TokenType.NOT_EQUAL,      "!="); pos += 2; }
                else                     { emit(TokenType.BANG,            "!"); pos++;    }
                break;
            case '<':
                if (peek(1) == '=')      { emit(TokenType.LESS_EQUAL,    "<="); pos += 2; }
                else                     { emit(TokenType.LESS,            "<"); pos++;    }
                break;
            case '>':
                if (peek(1) == '=')      { emit(TokenType.GREATER_EQUAL, ">="); pos += 2; }
                else                     { emit(TokenType.GREATER,         ">"); pos++;    }
                break;
            case '&':
                if (peek(1) == '&')      { emit(TokenType.AND_AND, "&&"); pos += 2; }
                else throw new LexerException("Unexpected '&' — did you mean '&&'?", line);
                break;
            case '|':
                if (peek(1) == '|')      { emit(TokenType.OR_OR,  "||"); pos += 2; }
                else throw new LexerException("Unexpected '|' — did you mean '||'?", line);
                break;
            case '(':  emit(TokenType.LPAREN,    "("); pos++; break;
            case ')':  emit(TokenType.RPAREN,    ")"); pos++; break;
            case '{':  emit(TokenType.LBRACE,    "{"); pos++; break;
            case '}':  emit(TokenType.RBRACE,    "}"); pos++; break;
            case '[':  emit(TokenType.LBRACKET,  "["); pos++; break;
            case ']':  emit(TokenType.RBRACKET,  "]"); pos++; break;
            case ';':  emit(TokenType.SEMICOLON, ";"); pos++; break;
            case ',':  emit(TokenType.COMMA,     ","); pos++; break;
            case '.':  emit(TokenType.DOT,       "."); pos++; break;
            default:
                throw new LexerException("Unexpected character '" + c + "'", line);
        }
    }

    private void scanString() throws LexerException {
        pos++; // skip opening "
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && source.charAt(pos) != '"') {
            char c = source.charAt(pos);
            if (c == '\n') throw new LexerException("Unterminated string literal", line);
            if (c == '\\') {
                pos++;
                if (pos >= source.length()) break;
                switch (source.charAt(pos)) {
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    case '"':  sb.append('"');  break;
                    case '\'': sb.append('\''); break;
                    case '\\': sb.append('\\'); break;
                    default:   sb.append('\\'); sb.append(source.charAt(pos));
                }
            } else {
                sb.append(c);
            }
            pos++;
        }
        if (pos >= source.length())
            throw new LexerException("Unterminated string literal — missing closing \"", line);
        pos++; // skip closing "
        emit(TokenType.STRING_LITERAL, sb.toString());
    }

    private void scanChar() throws LexerException {
        pos++;
        if (pos >= source.length()) throw new LexerException("Unterminated char literal", line);
        char value;
        if (source.charAt(pos) == '\\') {
            pos++;
            if (pos >= source.length()) throw new LexerException("Unterminated char literal", line);
            switch (source.charAt(pos)) {
                case 'n':  value = '\n'; break;
                case 't':  value = '\t'; break;
                case '\'': value = '\''; break;
                case '\\': value = '\\'; break;
                default:   value = source.charAt(pos);
            }
        } else {
            value = source.charAt(pos);
        }
        pos++;
        if (pos >= source.length() || source.charAt(pos) != '\'')
            throw new LexerException("Char literal must be exactly one character, e.g. 'A'", line);
        pos++;
        emit(TokenType.CHAR_LITERAL, String.valueOf(value));
    }

    private void scanNumber() {
        int start = pos;
        boolean isDouble = false;
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;

        if (pos < source.length() && source.charAt(pos) == '.'
                && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1))) {
            isDouble = true;
            pos++; // consume '.'
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
        }

        if (pos < source.length()) {
            char suffix = source.charAt(pos);
            if (suffix == 'f' || suffix == 'F' || suffix == 'd' || suffix == 'D') {
                isDouble = true;
                pos++;
            } else if (suffix == 'L' || suffix == 'l') {
                pos++;
            }
        }

        String raw = source.substring(start, pos);
        if (raw.endsWith("f") || raw.endsWith("F") ||
                raw.endsWith("d") || raw.endsWith("D")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        emit(isDouble ? TokenType.DOUBLE_LITERAL : TokenType.INTEGER_LITERAL, raw);
    }

    private void scanIdentifier() {
        int start = pos;
        while (pos < source.length()
                && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_'))
            pos++;
        String word = source.substring(start, pos);
        emit(keyword(word), word);
    }

    private TokenType keyword(String w) {
        switch (w) {
            case "public":    return TokenType.PUBLIC;
            case "private":   return TokenType.PRIVATE;
            case "protected": return TokenType.PROTECTED;
            case "static":    return TokenType.STATIC;
            case "final":     return TokenType.FINAL;
            case "void":      return TokenType.VOID;
            case "class":     return TokenType.CLASS;
            case "new":       return TokenType.NEW;
            case "return":    return TokenType.RETURN;
            case "int":       return TokenType.INT;
            case "double":    return TokenType.DOUBLE_TYPE;
            case "float":     return TokenType.FLOAT_TYPE;
            case "long":      return TokenType.LONG_TYPE;
            case "String":    return TokenType.STRING_TYPE;
            case "boolean":   return TokenType.BOOLEAN_TYPE;
            case "char":      return TokenType.CHAR_TYPE;
            case "if":        return TokenType.IF;
            case "else":      return TokenType.ELSE;
            case "while":     return TokenType.WHILE;
            case "for":       return TokenType.FOR;
            case "do":        return TokenType.DO;
            case "break":     return TokenType.BREAK;
            case "continue":  return TokenType.CONTINUE;
            case "true":
            case "false":     return TokenType.BOOLEAN_LITERAL;
            case "null":      return TokenType.NULL_LITERAL;
            default:          return TokenType.IDENTIFIER;
        }
    }

    private char peek(int offset) {
        int i = pos + offset;
        return i < source.length() ? source.charAt(i) : '\0';
    }

    private void emit(TokenType type, String value) {
        tokens.add(new Token(type, value, line));
    }
}