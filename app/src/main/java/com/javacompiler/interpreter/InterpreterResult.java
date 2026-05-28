package com.javacompiler.interpreter;

import java.util.ArrayList;
import java.util.List;

public class InterpreterResult {

    private final List<String> chunks = new ArrayList<>();
    private String errorMessage = null;
    private int    errorLine    = -1;

    public void addOutput(String chunk) { chunks.add(chunk); }

    public String getOutputText() {
        StringBuilder sb = new StringBuilder();
        for (String c : chunks) sb.append(c);
        String text = sb.toString();
        if (text.endsWith("\n")) text = text.substring(0, text.length() - 1);
        return text;
    }

    public void setError(String msg, int line) { this.errorMessage = msg; this.errorLine = line; }

    public boolean hasError()          { return errorMessage != null; }
    public String  getErrorMessage()   { return errorMessage; }
    public int     getErrorLine()      { return errorLine; }

    public String getFormattedError() {
        return errorLine >= 0
                ? "❌  Line " + errorLine + ":  " + errorMessage
                : "❌  " + errorMessage;
    }
}