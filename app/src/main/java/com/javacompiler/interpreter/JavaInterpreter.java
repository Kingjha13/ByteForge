package com.javacompiler.interpreter;

import java.util.List;

public class JavaInterpreter {

    private static class BreakSignal    extends RuntimeException { BreakSignal()    { super(null,null,true,false); } }
    private static class ContinueSignal extends RuntimeException { ContinueSignal() { super(null,null,true,false); } }
    private static class ReturnSignal   extends RuntimeException {
        final Object value;
        ReturnSignal(Object v) { super(null,null,true,false); this.value = v; }
    }

    private static final int MAX_ITERATIONS = 100_000;
    private InterpreterResult result;


    public InterpreterResult run(String source) {
        result = new InterpreterResult();
        try {
            List<Token> tokens = new Lexer(source).tokenize();
            ASTNode.Program program  = new Parser(tokens).parse();

            ASTNode.MethodDecl main = null;
            for (ASTNode.MethodDecl m : program.classDecl.methods) {
                if (m.name.equals("main")) { main = m; break; }
            }
            if (main == null) {
                result.setError(
                        "No 'main' method found.\n" +
                                "Every Java program needs: public static void main(String[] args) { ... }",
                        -1);
                return result;
            }
            try {
                execBlock(main.body, new Environment());
            } catch (ReturnSignal ignored) { /* void main returned */ }

        } catch (LexerException e) { result.setError("Syntax error: " + e.getMessage(), e.line);
        } catch (ParseException  e) { result.setError("Syntax error: " + e.getMessage(), e.line);
        } catch (RuntimeError    e) { result.setError(e.getMessage(), e.line);
        } catch (Exception       e) { result.setError("Internal error: " + e.getMessage(), -1); }

        return result;
    }


    private void execBlock(ASTNode.Block block, Environment parent) {
        Environment env = new Environment(parent);
        for (ASTNode.Statement s : block.statements) exec(s, env);
    }

    private void exec(ASTNode.Statement s, Environment env) {
        if (s instanceof ASTNode.VarDecl)       { execVarDecl     ((ASTNode.VarDecl)       s, env); return; }
        if (s instanceof ASTNode.PrintStatement){ execPrint        ((ASTNode.PrintStatement) s, env); return; }
        if (s instanceof ASTNode.ExprStatement) { eval(((ASTNode.ExprStatement) s).expr, env);       return; }
        if (s instanceof ASTNode.IfStatement)   { execIf          ((ASTNode.IfStatement)    s, env); return; }
        if (s instanceof ASTNode.WhileStatement){ execWhile        ((ASTNode.WhileStatement) s, env); return; }
        if (s instanceof ASTNode.ForStatement)  { execFor          ((ASTNode.ForStatement)   s, env); return; }
        if (s instanceof ASTNode.ReturnStatement) {
            ASTNode.ReturnStatement r = (ASTNode.ReturnStatement) s;
            throw new ReturnSignal(r.value != null ? eval(r.value, env) : null);
        }
        if (s instanceof ASTNode.BreakStatement)    throw new BreakSignal();
        if (s instanceof ASTNode.ContinueStatement) throw new ContinueSignal();
    }

    private void execVarDecl(ASTNode.VarDecl d, Environment env) {
        Object value = d.initializer != null
                ? coerce(eval(d.initializer, env), d.typeName, d.isArray, d.line)
                : defaultValue(d.typeName, d.isArray);
        env.define(d.name, value);
    }

    private void execPrint(ASTNode.PrintStatement s, Environment env) {
        String text = s.value == null ? "" : stringify(eval(s.value, env));
        result.addOutput(s.newline ? text + "\n" : text);
    }

    private void execIf(ASTNode.IfStatement s, Environment env) {
        if (truthy(eval(s.condition, env), s.line)) execBlock(s.thenBlock, env);
        else if (s.elseBlock != null)               execBlock(s.elseBlock, env);
    }

    private void execWhile(ASTNode.WhileStatement s, Environment env) {
        int count = 0;
        while (truthy(eval(s.condition, env), s.line)) {
            if (++count > MAX_ITERATIONS)
                throw new RuntimeError("Infinite loop detected (> " + MAX_ITERATIONS + " iterations)", s.line);
            try { execBlock(s.body, env); }
            catch (BreakSignal b)    { break; }
            catch (ContinueSignal c) { /* next iteration */ }
        }
    }

    private void execFor(ASTNode.ForStatement s, Environment env) {
        Environment forEnv = new Environment(env);
        if (s.init != null) exec(s.init, forEnv);
        int count = 0;
        while (s.condition == null || truthy(eval(s.condition, forEnv), s.line)) {
            if (++count > MAX_ITERATIONS)
                throw new RuntimeError("Infinite loop detected (> " + MAX_ITERATIONS + " iterations)", s.line);
            try { execBlock(s.body, forEnv); }
            catch (BreakSignal b)    { break; }
            catch (ContinueSignal c) { /* fall through to update */ }
            if (s.update != null) eval(s.update, forEnv);
        }
    }


    private Object eval(ASTNode.Expression e, Environment env) {
        if (e instanceof ASTNode.IntLiteral)    return ((ASTNode.IntLiteral)    e).value;
        if (e instanceof ASTNode.DoubleLiteral) return ((ASTNode.DoubleLiteral) e).value;
        if (e instanceof ASTNode.StringLiteral) return ((ASTNode.StringLiteral) e).value;
        if (e instanceof ASTNode.CharLiteral)   return ((ASTNode.CharLiteral)   e).value;
        if (e instanceof ASTNode.BoolLiteral)   return ((ASTNode.BoolLiteral)   e).value;
        if (e instanceof ASTNode.NullLiteral)   return null;
        if (e instanceof ASTNode.VarRef)        return env.get(((ASTNode.VarRef) e).name, e.line);
        if (e instanceof ASTNode.BinaryExpr)    return evalBinary     ((ASTNode.BinaryExpr)    e, env);
        if (e instanceof ASTNode.UnaryExpr)     return evalUnary      ((ASTNode.UnaryExpr)     e, env);
        if (e instanceof ASTNode.AssignExpr)    return evalAssign     ((ASTNode.AssignExpr)    e, env);
        if (e instanceof ASTNode.ArrayAccess)   return evalArrayAccess((ASTNode.ArrayAccess)   e, env);
        if (e instanceof ASTNode.ArrayCreation) return evalArrayCreate((ASTNode.ArrayCreation) e, env);
        if (e instanceof ASTNode.FieldAccess)   return evalFieldAccess((ASTNode.FieldAccess)   e, env);
        if (e instanceof ASTNode.MethodCall)    return evalMethodCall ((ASTNode.MethodCall)    e, env);
        throw new RuntimeError("Unknown expression node", e.line);
    }
    private Object evalBinary(ASTNode.BinaryExpr e, Environment env) {
        // Short-circuit logical
        if (e.op.equals("&&")) { return truthy(eval(e.left,env),e.line) && truthy(eval(e.right,env),e.line); }
        if (e.op.equals("||")) { return truthy(eval(e.left,env),e.line) || truthy(eval(e.right,env),e.line); }

        Object L = eval(e.left, env);
        Object R = eval(e.right, env);

        if (e.op.equals("+") && (L instanceof String || R instanceof String))
            return stringify(L) + stringify(R);

        switch (e.op) {
            case "+": case "-": case "*": case "/": case "%":
                return numOp(L, R, e.op, e.line);
            case "==": return isEqual(L, R);
            case "!=": return !isEqual(L, R);
            case "<":  return cmpNum(L, R, e.op, e.line);
            case ">":  return cmpNum(L, R, e.op, e.line);
            case "<=": return cmpNum(L, R, e.op, e.line);
            case ">=": return cmpNum(L, R, e.op, e.line);
        }
        throw new RuntimeError("Unknown operator '" + e.op + "'", e.line);
    }

    private Object numOp(Object L, Object R, String op, int line) {
        if (!(L instanceof Number) || !(R instanceof Number))
            throw new RuntimeError(
                    "Operator '" + op + "' requires numbers, got " + typeName(L) + " and " + typeName(R), line);

        boolean d = (L instanceof Double) || (R instanceof Double);
        if (d) {
            double l = toD(L, line), r = toD(R, line);
            switch (op) {
                case "+": return l + r;
                case "-": return l - r;
                case "*": return l * r;
                case "/": if (r == 0) throw new RuntimeError("Division by zero", line); return l / r;
                case "%": return l % r;
            }
        } else {
            int l = toI(L, line), r = toI(R, line);
            switch (op) {
                case "+": return l + r;
                case "-": return l - r;
                case "*": return l * r;
                case "/": if (r == 0) throw new RuntimeError("Division by zero", line); return l / r;
                case "%": if (r == 0) throw new RuntimeError("Modulo by zero", line);   return l % r;
            }
        }
        throw new RuntimeError("Unknown arithmetic op: " + op, line);
    }

    private boolean cmpNum(Object L, Object R, String op, int line) {
        if (L instanceof Character && R instanceof Character) {
            char l = (Character) L, r = (Character) R;
            switch (op) { case "<": return l<r; case ">": return l>r; case "<=": return l<=r; default: return l>=r; }
        }
        if (!(L instanceof Number) || !(R instanceof Number))
            throw new RuntimeError("Cannot compare " + typeName(L) + " and " + typeName(R), line);
        double l = toD(L, line), r = toD(R, line);
        switch (op) { case "<": return l<r; case ">": return l>r; case "<=": return l<=r; default: return l>=r; }
    }


    private Object evalUnary(ASTNode.UnaryExpr e, Environment env) {
        if (e.op.equals("!")) return !truthy(eval(e.operand, env), e.line);
        if (e.op.equals("-")) {
            Object v = eval(e.operand, env);
            if (v instanceof Integer) return -(Integer) v;
            if (v instanceof Double)  return -(Double)  v;
            throw new RuntimeError("Unary '-' requires a number", e.line);
        }
        int delta = e.op.equals("++") ? 1 : -1;
        Object cur = eval(e.operand, env);
        Object nxt;
        if      (cur instanceof Integer) nxt = (Integer) cur + delta;
        else if (cur instanceof Double)  nxt = (Double)  cur + delta;
        else throw new RuntimeError(e.op + " requires a numeric variable", e.line);
        assignTarget(e.operand, nxt, env, e.line);
        return e.prefix ? nxt : cur;
    }
    private Object evalAssign(ASTNode.AssignExpr e, Environment env) {
        Object rhs = eval(e.value, env);
        if (!e.op.equals("=")) {
            Object cur = eval(e.target, env);
            String op  = e.op.substring(0, e.op.length() - 1); // += → +
            rhs = (op.equals("+") && (cur instanceof String || rhs instanceof String))
                    ? stringify(cur) + stringify(rhs)
                    : numOp(cur, rhs, op, e.line);
        }
        assignTarget(e.target, rhs, env, e.line);
        return rhs;
    }

    private void assignTarget(ASTNode.Expression target, Object value, Environment env, int line) {
        if (target instanceof ASTNode.VarRef) {
            env.set(((ASTNode.VarRef) target).name, value, line);
        } else if (target instanceof ASTNode.ArrayAccess) {
            ASTNode.ArrayAccess a = (ASTNode.ArrayAccess) target;
            Object[] arr = requireArray(eval(a.array, env), line);
            int idx = toI(eval(a.index, env), line);
            checkBounds(idx, arr.length, line);
            arr[idx] = value;
        } else {
            throw new RuntimeError("Invalid assignment target", line);
        }
    }
    private Object evalArrayAccess(ASTNode.ArrayAccess e, Environment env) {
        Object[] arr = requireArray(eval(e.array, env), e.line);
        int idx = toI(eval(e.index, env), e.line);
        checkBounds(idx, arr.length, e.line);
        return arr[idx];
    }

    private Object evalArrayCreate(ASTNode.ArrayCreation e, Environment env) {
        if (e.items != null) {
            Object[] arr = new Object[e.items.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = eval(e.items.get(i), env);
            return arr;
        }
        int size = toI(eval(e.size, env), e.line);
        if (size < 0) throw new RuntimeError("Negative array size: " + size, e.line);
        Object[] arr = new Object[size];
        Object def   = defaultValue(e.elementType, false);
        for (int i = 0; i < size; i++) arr[i] = def;
        return arr;
    }


    private Object evalFieldAccess(ASTNode.FieldAccess e, Environment env) {
        Object obj = eval(e.object, env);
        if (obj instanceof Object[] && e.field.equals("length")) return ((Object[]) obj).length;
        throw new RuntimeError("Unknown field '" + e.field + "' on " + typeName(obj), e.line);
    }


    private Object evalMethodCall(ASTNode.MethodCall e, Environment env) {
        if (e.object == null)
            throw new RuntimeError("Unknown function '" + e.method + "'", e.line);

        Object obj = eval(e.object, env);

        if (obj instanceof String) return callStringMethod((String) obj, e.method, e.args, env, e.line);
        if (obj instanceof Object[]) {
            throw new RuntimeError("Arrays don't have method '" + e.method +
                    "'. Use .length (no parentheses) for the array length.", e.line);
        }
        throw new RuntimeError("Cannot call '" + e.method + "' on " + typeName(obj), e.line);
    }

    private Object callStringMethod(String s, String method,
                                    List<ASTNode.Expression> args, Environment env, int line) {
        switch (method) {
            case "length":      return s.length();
            case "toUpperCase": return s.toUpperCase();
            case "toLowerCase": return s.toLowerCase();
            case "trim":        return s.trim();
            case "isEmpty":     return s.isEmpty();
            case "equals":      return s.equals(stringArg(args, 0, env, line));
            case "contains":    return s.contains(stringArg(args, 0, env, line));
            case "startsWith":  return s.startsWith(stringArg(args, 0, env, line));
            case "endsWith":    return s.endsWith(stringArg(args, 0, env, line));
            case "replace":     return s.replace(stringArg(args, 0, env, line), stringArg(args, 1, env, line));
            case "indexOf": {
                Object a = eval(requireArg(args, 0, "indexOf", line), env);
                return (a instanceof Character) ? s.indexOf((Character)a) : s.indexOf(stringify(a));
            }
            case "charAt": {
                int idx = toI(eval(requireArg(args, 0, "charAt", line), env), line);
                if (idx < 0 || idx >= s.length())
                    throw new RuntimeError("charAt index " + idx + " out of bounds (length " + s.length() + ")", line);
                return s.charAt(idx);
            }
            case "substring": {
                if (args.size() == 1) return s.substring(toI(eval(args.get(0), env), line));
                if (args.size() == 2) return s.substring(toI(eval(args.get(0),env),line), toI(eval(args.get(1),env),line));
                throw new RuntimeError("substring requires 1 or 2 arguments", line);
            }
            default:
                throw new RuntimeError("Unknown String method '" + method + "'", line);
        }
    }



    private boolean truthy(Object v, int line) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Integer) return (Integer) v != 0;
        if (v instanceof Double)  return (Double)  v != 0.0;
        throw new RuntimeError(
                "Expected boolean in condition, got " + typeName(v), line);
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number)
            return toD(a, 0) == toD(b, 0);
        return a.equals(b);
    }

    private int    toI(Object v, int l) {
        if (v instanceof Integer)   return (Integer)   v;
        if (v instanceof Double)    return ((Double)   v).intValue();
        if (v instanceof Character) return (int)(Character) v;
        throw new RuntimeError("Expected int, got " + typeName(v), l);
    }
    private double toD(Object v, int l) {
        if (v instanceof Integer)   return ((Integer)   v).doubleValue();
        if (v instanceof Double)    return (Double)     v;
        if (v instanceof Character) return (double)(Character) v;
        throw new RuntimeError("Expected number, got " + typeName(v), l);
    }

    private Object[] requireArray(Object v, int line) {
        if (!(v instanceof Object[]))
            throw new RuntimeError("Expected array, got " + typeName(v), line);
        return (Object[]) v;
    }

    private void checkBounds(int idx, int len, int line) {
        if (idx < 0 || idx >= len)
            throw new RuntimeError(
                    "ArrayIndexOutOfBoundsException: index " + idx + " out of bounds for length " + len, line);
    }

    private ASTNode.Expression requireArg(List<ASTNode.Expression> args, int i, String method, int line) {
        if (i >= args.size())
            throw new RuntimeError("'" + method + "' requires at least " + (i+1) + " argument(s)", line);
        return args.get(i);
    }

    private String stringArg(List<ASTNode.Expression> args, int i, Environment env, int line) {
        return stringify(eval(requireArg(args, i, "", line), env));
    }

    private String stringify(Object v) {
        if (v == null)            return "null";
        if (v instanceof Object[]) {
            Object[] arr = (Object[]) v;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(stringify(arr[i]));
            }
            return sb.append("]").toString();
        }
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15)
                return ((long) d) + ".0";
        }
        return v.toString();
    }

    private Object coerce(Object v, String type, boolean isArray, int line) {
        if (isArray) return v;
        switch (type) {
            case "int":   case "long":  if (v instanceof Double)  return ((Double)v).intValue();   return v;
            case "double":case "float": if (v instanceof Integer) return ((Integer)v).doubleValue(); return v;
            default: return v;
        }
    }

    private Object defaultValue(String type, boolean isArray) {
        if (isArray) return null;
        switch (type) {
            case "int":  case "long":               return 0;
            case "double": case "float":             return 0.0;
            case "boolean":                          return false;
            case "char":                             return '\0';
            default:                                 return null;
        }
    }

    private String typeName(Object v) {
        if (v == null)            return "null";
        if (v instanceof Integer) return "int";
        if (v instanceof Double)  return "double";
        if (v instanceof Boolean) return "boolean";
        if (v instanceof Character) return "char";
        if (v instanceof String)  return "String";
        if (v instanceof Object[]) return "array";
        return v.getClass().getSimpleName();
    }
}