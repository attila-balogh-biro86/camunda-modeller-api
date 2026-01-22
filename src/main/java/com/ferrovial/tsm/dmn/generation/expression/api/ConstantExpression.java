package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.Collections;
import java.util.List;

/**
 * Represents a constant expression that always evaluates to true or false.
 * Useful for "Always" conditions or default rules.
 */
public class ConstantExpression implements Expression {

    public static final ConstantExpression TRUE = new ConstantExpression(true, "Always");
    public static final ConstantExpression FALSE = new ConstantExpression(false, "Never");

    private final boolean value;
    private final String label;

    private ConstantExpression(boolean value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Returns the constant boolean value.
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Returns the label for this constant.
     */
    public String getLabel() {
        return label;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.CONSTANT;
    }

    @Override
    public String toReadableString() {
        return label;
    }

    @Override
    public ValidationResult validate() {
        return ValidationResult.valid();
    }

    @Override
    public List<String> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitConstant(this);
    }

    @Override
    public Expression copy() {
        return this; // Immutable, safe to return same instance
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Creates a constant expression that always evaluates to true.
     */
    public static ConstantExpression alwaysTrue() {
        return TRUE;
    }

    /**
     * Creates a constant expression that always evaluates to true with a custom label.
     */
    public static ConstantExpression alwaysTrue(String label) {
        return new ConstantExpression(true, label);
    }

    /**
     * Creates a constant expression that always evaluates to false.
     */
    public static ConstantExpression alwaysFalse() {
        return FALSE;
    }

    /**
     * Creates a constant expression that always evaluates to false with a custom label.
     */
    public static ConstantExpression alwaysFalse(String label) {
        return new ConstantExpression(false, label);
    }
}
