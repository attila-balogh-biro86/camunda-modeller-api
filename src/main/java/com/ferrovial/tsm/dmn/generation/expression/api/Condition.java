package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single condition comparing a parameter to a value.
 * This is the fundamental building block of expressions.
 *
 * <p>Example conditions:</p>
 * <ul>
 *   <li>amount > 100</li>
 *   <li>status == "active"</li>
 *   <li>name contains "truck"</li>
 *   <li>notes isEmpty</li>
 * </ul>
 */
public class Condition implements Expression {

    private final String parameterName;
    private final Operator operator;
    private final String value;
    private final DataType dataType;
    private final boolean grouped; // Has surrounding parentheses

    private Condition(Builder builder) {
        this.parameterName = Objects.requireNonNull(builder.parameterName, "Parameter name required");
        this.operator = Objects.requireNonNull(builder.operator, "Operator required");
        this.value = builder.value != null ? builder.value : "";
        this.dataType = builder.dataType != null ? builder.dataType : DataType.inferFromValue(this.value);
        this.grouped = builder.grouped;
    }

    /**
     * Returns the parameter (variable) name.
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Returns the comparison operator.
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Returns the comparison value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the data type of the parameter.
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Returns true if this condition has surrounding parentheses.
     */
    public boolean isGrouped() {
        return grouped;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.CONDITION;
    }

    @Override
    public String toReadableString() {
        StringBuilder sb = new StringBuilder();
        if (grouped) sb.append("(");
        sb.append(parameterName).append(" ").append(operator.getSymbol());
        if (operator.requiresValue()) {
            sb.append(" ").append(value);
        }
        if (grouped) sb.append(")");
        return sb.toString();
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();

        if (parameterName == null || parameterName.trim().isEmpty()) {
            result.addError("Parameter name is required");
        }

        if (operator == null) {
            result.addError("Operator is required");
        } else {
            if (!operator.supportsDataType(dataType)) {
                result.addError("Operator '" + operator.getDisplayName() +
                        "' does not support data type " + dataType);
            }
            if (operator.requiresValue() && (value == null || value.trim().isEmpty())) {
                result.addError("Value is required for operator '" + operator.getDisplayName() + "'");
            }
        }

        return result.build();
    }

    @Override
    public List<String> getParameters() {
        return Collections.singletonList(parameterName);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitCondition(this);
    }

    @Override
    public Expression copy() {
        return new Builder(this.parameterName, this.operator)
                .value(this.value)
                .dataType(this.dataType)
                .grouped(this.grouped)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Condition condition = (Condition) o;
        return parameterName.equals(condition.parameterName) &&
                operator == condition.operator &&
                Objects.equals(value, condition.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterName, operator, value);
    }

    @Override
    public String toString() {
        return toReadableString();
    }

    // ========================================================================
    // Static Factory Methods
    // ========================================================================

    /**
     * Creates a condition: parameter == value
     */
    public static Condition equals(String parameter, String value) {
        return new Builder(parameter, Operator.EQUALS).value(value).grouped(true).build();
    }

    /**
     * Creates a condition: parameter != value
     */
    public static Condition notEquals(String parameter, String value) {
        return new Builder(parameter, Operator.NOT_EQUALS).value(value).grouped(true).build();
    }

    /**
     * Creates a condition: parameter > value
     */
    public static Condition greaterThan(String parameter, String value) {
        return new Builder(parameter, Operator.GREATER_THAN).value(value).grouped(true).build();
    }

    /**
     * Creates a condition: parameter >= value
     */
    public static Condition greaterThanOrEqual(String parameter, String value) {
        return new Builder(parameter, Operator.GREATER_THAN_OR_EQUAL).value(value).grouped(true).build();
    }

    /**
     * Creates a condition: parameter < value
     */
    public static Condition lessThan(String parameter, String value) {
        return new Builder(parameter, Operator.LESS_THAN).value(value).grouped(true).build();
    }

    /**
     * Creates a condition: parameter <= value
     */
    public static Condition lessThanOrEqual(String parameter, String value) {
        return new Builder(parameter, Operator.LESS_THAN_OR_EQUAL).value(value).grouped(true).build();
    }

    /**
     * Creates a condition: parameter contains value
     */
    public static Condition contains(String parameter, String value) {
        return new Builder(parameter, Operator.CONTAINS).value(value).dataType(DataType.STRING).grouped(true).build();
    }

    /**
     * Creates a condition: parameter startsWith value
     */
    public static Condition startsWith(String parameter, String value) {
        return new Builder(parameter, Operator.STARTS_WITH).value(value).dataType(DataType.STRING).grouped(true).build();
    }

    /**
     * Creates a condition: parameter isEmpty
     */
    public static Condition isEmpty(String parameter) {
        return new Builder(parameter, Operator.IS_EMPTY).dataType(DataType.STRING).grouped(true).build();
    }

    /**
     * Creates a condition: parameter isNull
     */
    public static Condition isNull(String parameter) {
        return new Builder(parameter, Operator.IS_NULL).grouped(true).build();
    }

    /**
     * Creates a builder for constructing a Condition.
     */
    public static Builder builder(String parameterName, Operator operator) {
        return new Builder(parameterName, operator);
    }

    /**
     * Builder for Condition.
     */
    public static class Builder {
        private final String parameterName;
        private final Operator operator;
        private String value;
        private DataType dataType;
        private boolean grouped = true;

        private Builder(String parameterName, Operator operator) {
            this.parameterName = parameterName;
            this.operator = operator;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder dataType(DataType dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder grouped(boolean grouped) {
            this.grouped = grouped;
            return this;
        }

        public Condition build() {
            return new Condition(this);
        }
    }
}
