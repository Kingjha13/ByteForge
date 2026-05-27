package com.javacompiler.interpreter;

import java.util.List;
import java.util.ArrayList;

public class InterpreterResult {

    private final List<String> outputLines = new ArrayList<>();

    private String errorMessage = null;
    private int errorLine = -1;

    public void addOutput(String line) {
        outputLines.add(line);
    }

    public List<String> getOutputLines() {
        return outputLines;
    }

    public String getOutputText() {
        StringBuilder sb = new StringBuilder();
        for (String line : outputLines) {
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    public void setError(String message, int line) {
        this.errorMessage = message;
        this.errorLine = line;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorLine() {
        return errorLine;
    }
    public String getFormattedError() {
        if (errorLine >= 0) {
            return "❌ Error on line " + errorLine + ": " + errorMessage;
        }
        return "❌ Error: " + errorMessage;
    }
}