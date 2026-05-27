package com.javacompiler.interpreter;


public class JavaInterpreter {

    public InterpreterResult run(String sourceCode) {
        InterpreterResult result = new InterpreterResult();

        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            result.setError("No code to run.", -1);
            return result;
        }

        String[] lines = sourceCode.split("\n");

        boolean insideMain = false;
        int braceDepth = 0;

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String raw = lines[i];
            String trimmed = raw.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue;

            if (trimmed.contains("public static void main")) {
                insideMain = true;
            }

            for (char c : trimmed.toCharArray()) {
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
            }

            if (!insideMain) continue;
            if (braceDepth <= 0 && insideMain) {
                insideMain = false; // exited main
                continue;
            }

            if (trimmed.contains("public static void main")) continue;
            if (trimmed.equals("{")) continue;

            boolean handled = executeStatement(trimmed, lineNumber, result);

            if (result.hasError()) break;

            if (!handled && !trimmed.equals("}") && !trimmed.isEmpty()) {
                result.setError(
                        "Unknown statement: \"" + trimmed + "\". " +
                                "(Tip: Only System.out.println() is supported in Phase 1)",
                        lineNumber
                );
                break;
            }
        }

        if (!insideMain && result.getOutputLines().isEmpty() && !result.hasError()) {
            if (!sourceCode.contains("public static void main")) {
                result.setError(
                        "No main method found. " +
                                "Every Java program needs: public static void main(String[] args)",
                        -1
                );
            }
        }

        return result;
    }


    private boolean executeStatement(String stmt, int lineNumber, InterpreterResult result) {

        if (stmt.startsWith("System.out.println(")) {
            String content = extractPrintContent(stmt, "System.out.println(", lineNumber, result);
            if (content != null) {
                result.addOutput(content + "\n");   // println adds newline
            }
            return true;
        }

        if (stmt.startsWith("System.out.print(") && !stmt.startsWith("System.out.println(")) {
            String content = extractPrintContent(stmt, "System.out.print(", lineNumber, result);
            if (content != null) {
                result.addOutput(content);
            }
            return true;
        }

        return false;
    }

    private String extractPrintContent(String stmt,
                                       String prefix,
                                       int lineNumber,
                                       InterpreterResult result) {

        if (!stmt.endsWith(");")) {
            result.setError("Missing semicolon or closing parenthesis — did you mean: " + stmt + ");", lineNumber);
            return null;
        }

        String inner = stmt.substring(prefix.length(), stmt.length() - 2).trim();

        if (inner.startsWith("\"") && inner.endsWith("\"")) {
            return inner.substring(1, inner.length() - 1); // strip quotes
        }

        if (inner.isEmpty()) {
            return "";
        }

        if (inner.startsWith("'") && inner.endsWith("'") && inner.length() == 3) {
            return String.valueOf(inner.charAt(1));
        }

        try {
            int val = Integer.parseInt(inner);
            return String.valueOf(val);
        } catch (NumberFormatException ignored) {}

        try {
            double val = Double.parseDouble(inner);
            return String.valueOf(val);
        } catch (NumberFormatException ignored) {}

        result.setError(
                "Cannot print \"" + inner + "\" — " +
                        "in Phase 1 only String literals like println(\"Hello\") are supported.",
                lineNumber
        );
        return null;
    }
}