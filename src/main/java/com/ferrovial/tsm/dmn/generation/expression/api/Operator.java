package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines all operators supported for building conditions.
 * Operators are categorized by the data types they support.
 */
public enum Operator {

    // ========================================================================
    // Comparison Operators (work with all types)
    // ========================================================================

    EQUALS("==", "equals", Category.COMPARISON, true, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return dataType == DataType.STRING ? "\"" + value + "\"" : value;
        }
    },

    NOT_EQUALS("!=", "not equals", Category.COMPARISON, true, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return dataType == DataType.STRING ? "not(\"" + value + "\")" : "not(" + value + ")";
        }
    },

    // ========================================================================
    // Numeric Operators (INTEGER, DOUBLE, LONG)
    // ========================================================================

    GREATER_THAN(">", "greater than", Category.NUMERIC, true, false) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "> " + value;
        }
    },

    GREATER_THAN_OR_EQUAL(">=", "greater than or equal", Category.NUMERIC, true, false) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return ">= " + value;
        }
    },

    LESS_THAN("<", "less than", Category.NUMERIC, true, false) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "< " + value;
        }
    },

    LESS_THAN_OR_EQUAL("<=", "less than or equal", Category.NUMERIC, true, false) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "<= " + value;
        }
    },

    BETWEEN("between", "between", Category.NUMERIC, true, false) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            // Expects value in format "min,max"
            String[] parts = value.split(",");
            if (parts.length == 2) {
                return "[" + parts[0].trim() + ".." + parts[1].trim() + "]";
            }
            return value;
        }
    },

    // ========================================================================
    // String Operators (STRING only)
    // ========================================================================

    CONTAINS("contains", "contains", Category.STRING, false, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "contains(?, \"" + value + "\")";
        }
    },

    NOT_CONTAINS("not contains", "does not contain", Category.STRING, false, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "not(contains(?, \"" + value + "\"))";
        }
    },

    STARTS_WITH("startsWith", "starts with", Category.STRING, false, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "starts with \"" + value + "\"";
        }
    },

    ENDS_WITH("endsWith", "ends with", Category.STRING, false, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "ends with \"" + value + "\"";
        }
    },

    MATCHES("matches", "matches regex", Category.STRING, false, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "matches(?, \"" + value + "\")";
        }
    },

    EQUALS_IGNORE_CASE("equalsIgnoreCase", "equals (ignore case)", Category.STRING, false, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "lower case(?) = \"" + value.toLowerCase() + "\"";
        }
    },

    // ========================================================================
    // Null/Empty Operators (no value required)
    // ========================================================================

    IS_NULL("isNull", "is null", Category.NULL_CHECK, true, true) {
        @Override
        public boolean requiresValue() {
            return false;
        }

        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "null";
        }
    },

    IS_NOT_NULL("isNotNull", "is not null", Category.NULL_CHECK, true, true) {
        @Override
        public boolean requiresValue() {
            return false;
        }

        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "not(null)";
        }
    },

    IS_EMPTY("isEmpty", "is empty", Category.NULL_CHECK, false, true) {
        @Override
        public boolean requiresValue() {
            return false;
        }

        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "null";
        }
    },

    IS_NOT_EMPTY("isNotEmpty", "is not empty", Category.NULL_CHECK, false, true) {
        @Override
        public boolean requiresValue() {
            return false;
        }

        @Override
        public String toFeelExpression(String value, DataType dataType) {
            return "not(null)";
        }
    },

    // ========================================================================
    // Collection Operators
    // ========================================================================

    IN("in", "in list", Category.COLLECTION, true, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            // Expects comma-separated values
            String[] values = value.split(",");
            if (dataType == DataType.STRING) {
                return Arrays.stream(values)
                        .map(v -> "\"" + v.trim() + "\"")
                        .collect(Collectors.joining(", "));
            }
            return Arrays.stream(values)
                    .map(String::trim)
                    .collect(Collectors.joining(", "));
        }
    },

    NOT_IN("notIn", "not in list", Category.COLLECTION, true, true) {
        @Override
        public String toFeelExpression(String value, DataType dataType) {
            String inExpr = IN.toFeelExpression(value, dataType);
            return "not(" + inExpr + ")";
        }
    };

    // ========================================================================
    // Enum Fields and Methods
    // ========================================================================

    private final String symbol;
    private final String displayName;
    private final Category category;
    private final boolean supportsNumeric;
    private final boolean supportsString;

    Operator(String symbol, String displayName, Category category,
             boolean supportsNumeric, boolean supportsString) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.category = category;
        this.supportsNumeric = supportsNumeric;
        this.supportsString = supportsString;
    }

    /**
     * Returns the symbol used in expressions (e.g., "==", ">", "contains").
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns a human-readable display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the category of this operator.
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Returns true if this operator requires a value to compare against.
     * Most operators require a value, except IS_NULL, IS_EMPTY, etc.
     */
    public boolean requiresValue() {
        return true;
    }

    /**
     * Returns true if this operator supports numeric types (INTEGER, DOUBLE, LONG).
     */
    public boolean supportsNumeric() {
        return supportsNumeric;
    }

    /**
     * Returns true if this operator supports STRING type.
     */
    public boolean supportsString() {
        return supportsString;
    }

    /**
     * Checks if this operator supports the given data type.
     */
    public boolean supportsDataType(DataType dataType) {
        if (dataType == null) return true;
        switch (dataType) {
            case INTEGER:
            case LONG:
            case DOUBLE:
                return supportsNumeric;
            case STRING:
                return supportsString;
            case BOOLEAN:
                return this == EQUALS || this == NOT_EQUALS || this == IS_NULL || this == IS_NOT_NULL;
            case DATE:
            case DATETIME:
                return supportsNumeric; // Date comparisons use numeric operators
            default:
                return true;
        }
    }

    /**
     * Converts this operator and value to a FEEL expression for DMN.
     *
     * @param value    the comparison value
     * @param dataType the data type of the parameter
     * @return FEEL expression string
     */
    public abstract String toFeelExpression(String value, DataType dataType);

    /**
     * Returns all operators that support the given data type.
     */
    public static List<Operator> forDataType(DataType dataType) {
        return Arrays.stream(values())
                .filter(op -> op.supportsDataType(dataType))
                .collect(Collectors.toList());
    }

    /**
     * Finds an operator by its symbol.
     */
    public static Operator fromSymbol(String symbol) {
        for (Operator op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operator symbol: " + symbol);
    }

    /**
     * Operator categories for grouping in UI.
     */
    public enum Category {
        COMPARISON("Comparison"),
        NUMERIC("Numeric"),
        STRING("String"),
        NULL_CHECK("Null/Empty"),
        COLLECTION("Collection");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
