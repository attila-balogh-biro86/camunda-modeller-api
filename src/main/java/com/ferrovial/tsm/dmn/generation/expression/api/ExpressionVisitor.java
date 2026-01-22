package com.ferrovial.tsm.dmn.generation.expression.api;

/**
 * Visitor interface for traversing and processing expressions.
 * Implements the Visitor pattern for extensible expression processing.
 *
 * @param <T> the return type of the visit methods
 */
public interface ExpressionVisitor<T> {

    /**
     * Visits a simple condition expression.
     */
    T visitCondition(Condition condition);

    /**
     * Visits a composite expression (multiple expressions combined with AND/OR).
     */
    T visitComposite(CompositeExpression composite);

    /**
     * Visits a grouped expression (parenthesized).
     */
    T visitGroup(GroupExpression group);

    /**
     * Visits a constant expression (always true/false).
     */
    T visitConstant(ConstantExpression constant);
}
