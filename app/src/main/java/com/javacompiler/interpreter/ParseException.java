package com.javacompiler.interpreter;

public class ParseException extends Exception {
    public final int line;
    public ParseException(String message, int line) {
        super(message);
        this.line = line;
    }
}