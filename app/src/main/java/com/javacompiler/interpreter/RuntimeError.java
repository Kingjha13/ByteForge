package com.javacompiler.interpreter;

public class RuntimeError extends RuntimeException {
    public final int line;
    public RuntimeError(String message, int line) {
        super(message);
        this.line = line;
    }
}