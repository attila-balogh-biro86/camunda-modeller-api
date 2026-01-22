package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.List;

/**
 * Base interface for all expression types.
 * An expression can be a simple condition or a composite of multiple conditions.
 *
 * <p>This API is designed to be framework-agnostic and can be used
 * independently of any UI framework (Vaadin, Swing, etc.).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Expression expr = ExpressionBuilder.create()
 *     .condition("amount", Operator.GREATER_THAN, "100")
 *     .and()
 *     .condition("status", Operator.EQUALS, "active")
 *     .build();
 *
 * String readable = expr.toReadableString();
 * // Output: (amount > 100) AND (status == "active")
 * }</pre>
 */
public interface Expression {

    /**
     * Returns the type of this expression.
     */
    ExpressionType getType();

    /**
     * Converts this expression to a human-readable string format.
     * Example: "(amount > 100) AND (status == active)"
     */
    String toReadableString();

    /**
     * Validates this expression.
     *
     * @return ValidationResult containing any errors or warnings
     */
    ValidationResult validate();

    /**
     * Returns all parameters (variable names) used in this expression.
     */
    List<String> getParameters();

    /**
     * Accepts a visitor for processing this expression.
     *
     * @param visitor the visitor to accept
     * @param <T> the return type of the visitor
     * @return the result from the visitor
     */
    <T> T accept(ExpressionVisitor<T> visitor);

    /**
     * Creates a deep copy of this expression.
     */
    Expression copy();
}
