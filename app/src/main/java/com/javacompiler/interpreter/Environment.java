package com.javacompiler.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private final Map<String, Object> vars = new HashMap<>();
    private final Environment parent;

    public Environment()                    { this.parent = null; }
    public Environment(Environment parent) { this.parent = parent; }

    public void define(String name, Object value) {
        vars.put(name, value);
    }

    public Object get(String name, int line) {
        if (vars.containsKey(name)) return vars.get(name);
        if (parent != null)         return parent.get(name, line);
        throw new RuntimeError("Undefined variable '" + name + "'", line);
    }

    public void set(String name, Object value, int line) {
        if (vars.containsKey(name)) { vars.put(name, value); return; }
        if (parent != null)         { parent.set(name, value, line); return; }
        throw new RuntimeError("Variable '" + name + "' has not been declared", line);
    }
}