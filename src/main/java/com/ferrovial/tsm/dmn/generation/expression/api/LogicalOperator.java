package com.ferrovial.tsm.dmn.generation.expression.api;

/**
 * Logical operators for combining expressions.
 */
public enum LogicalOperator {

    AND("and", "AND", "&&"),
    OR("or", "OR", "||");

    private final String symbol;
    private final String displayName;
    private final String javaSymbol;

    LogicalOperator(String symbol, String displayName, String javaSymbol) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.javaSymbol = javaSymbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getJavaSymbol() {
        return javaSymbol;
    }

    /**
     * Finds a LogicalOperator by its symbol (case-insensitive).
     */
    public static LogicalOperator fromSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }
        for (LogicalOperator op : values()) {
            if (op.symbol.equalsIgnoreCase(symbol.trim())) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown logical operator: " + symbol);
    }
}
