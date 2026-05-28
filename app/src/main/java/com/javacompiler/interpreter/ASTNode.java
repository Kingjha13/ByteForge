package com.javacompiler.interpreter;

import java.util.List;

public abstract class ASTNode {

    public int line;


    public static class Program extends ASTNode {
        public ClassDecl classDecl;
        public Program(ClassDecl classDecl, int line) {
            this.classDecl = classDecl; this.line = line;
        }
    }

    public static class ClassDecl extends ASTNode {
        public String name;
        public List<MethodDecl> methods;
        public ClassDecl(String name, List<MethodDecl> methods, int line) {
            this.name = name; this.methods = methods; this.line = line;
        }
    }

    public static class MethodDecl extends ASTNode {
        public String name;
        public Block body;
        public MethodDecl(String name, Block body, int line) {
            this.name = name; this.body = body; this.line = line;
        }
    }

    public static class Block extends ASTNode {
        public List<Statement> statements;
        public Block(List<Statement> statements, int line) {
            this.statements = statements; this.line = line;
        }
    }

    public abstract static class Statement extends ASTNode {}

    public static class VarDecl extends Statement {
        public String typeName;
        public boolean isArray;
        public String name;
        public Expression initializer;
        public VarDecl(String typeName, boolean isArray,
                       String name, Expression initializer, int line) {
            this.typeName = typeName; this.isArray = isArray;
            this.name = name; this.initializer = initializer; this.line = line;
        }
    }

    public static class ExprStatement extends Statement {
        public Expression expr;
        public ExprStatement(Expression expr, int line) {
            this.expr = expr; this.line = line;
        }
    }

    public static class PrintStatement extends Statement {
        public Expression value;
        public boolean newline;
        public PrintStatement(Expression value, boolean newline, int line) {
            this.value = value; this.newline = newline; this.line = line;
        }
    }

    public static class IfStatement extends Statement {
        public Expression condition;
        public Block thenBlock;
        public Block elseBlock;
        public IfStatement(Expression condition,
                           Block thenBlock, Block elseBlock, int line) {
            this.condition = condition;
            this.thenBlock = thenBlock; this.elseBlock = elseBlock; this.line = line;
        }
    }

    public static class WhileStatement extends Statement {
        public Expression condition;
        public Block body;
        public WhileStatement(Expression condition, Block body, int line) {
            this.condition = condition; this.body = body; this.line = line;
        }
    }

    public static class ForStatement extends Statement {
        public Statement init;
        public Expression condition;
        public Expression update;
        public Block body;
        public ForStatement(Statement init, Expression condition,
                            Expression update, Block body, int line) {
            this.init = init; this.condition = condition;
            this.update = update; this.body = body; this.line = line;
        }
    }

    public static class ReturnStatement extends Statement {
        public Expression value;
        public ReturnStatement(Expression value, int line) {
            this.value = value; this.line = line;
        }
    }

    public static class BreakStatement    extends Statement { public BreakStatement(int l)    { line = l; } }
    public static class ContinueStatement extends Statement { public ContinueStatement(int l) { line = l; } }


    public abstract static class Expression extends ASTNode {}

    public static class IntLiteral     extends Expression { public int    value; public IntLiteral   (int v, int l)    { value=v; line=l; } }
    public static class DoubleLiteral  extends Expression { public double value; public DoubleLiteral(double v, int l) { value=v; line=l; } }
    public static class StringLiteral  extends Expression { public String value; public StringLiteral(String v, int l) { value=v; line=l; } }
    public static class CharLiteral    extends Expression { public char   value; public CharLiteral  (char v, int l)   { value=v; line=l; } }
    public static class BoolLiteral    extends Expression { public boolean value; public BoolLiteral (boolean v, int l){ value=v; line=l; } }
    public static class NullLiteral    extends Expression { public NullLiteral(int l) { line=l; } }

    public static class VarRef extends Expression {
        public String name;
        public VarRef(String name, int line) { this.name = name; this.line = line; }
    }

    public static class BinaryExpr extends Expression {
        public Expression left, right;
        public String op;
        public BinaryExpr(Expression left, String op, Expression right, int line) {
            this.left = left; this.op = op; this.right = right; this.line = line;
        }
    }

    public static class UnaryExpr extends Expression {
        public String op;
        public Expression operand;
        public boolean prefix;
        public UnaryExpr(String op, Expression operand, boolean prefix, int line) {
            this.op = op; this.operand = operand; this.prefix = prefix; this.line = line;
        }
    }

    public static class AssignExpr extends Expression {
        public Expression target;
        public String op;
        public Expression value;
        public AssignExpr(Expression target, String op, Expression value, int line) {
            this.target = target; this.op = op; this.value = value; this.line = line;
        }
    }

    public static class ArrayAccess extends Expression {
        public Expression array, index;
        public ArrayAccess(Expression array, Expression index, int line) {
            this.array = array; this.index = index; this.line = line;
        }
    }

    public static class ArrayCreation extends Expression {
        public String elementType;
        public Expression size;
        public List<Expression> items;
        public ArrayCreation(String elementType, Expression size, List<Expression> items, int line) {
            this.elementType = elementType; this.size = size; this.items = items; this.line = line;
        }
    }

    public static class FieldAccess extends Expression {
        public Expression object;
        public String field;
        public FieldAccess(Expression object, String field, int line) {
            this.object = object; this.field = field; this.line = line;
        }
    }

    public static class MethodCall extends Expression {
        public Expression object;
        public String method;
        public List<Expression> args;
        public MethodCall(Expression object, String method, List<Expression> args, int line) {
            this.object = object; this.method = method; this.args = args; this.line = line;
        }
    }
}