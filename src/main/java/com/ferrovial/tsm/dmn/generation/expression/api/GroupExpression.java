package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.List;
import java.util.Objects;

/**
 * Represents a grouped (parenthesized) expression.
 * Used to control operator precedence.
 *
 * <p>Example:</p>
 * <pre>
 * ((a > 1) AND (b < 2)) OR (c == 3)
 * </pre>
 */
public class GroupExpression implements Expression {

    private final Expression inner;

    public GroupExpression(Expression inner) {
        this.inner = Objects.requireNonNull(inner, "Inner expression cannot be null");
    }

    /**
     * Returns the inner expression.
     */
    public Expression getInner() {
        return inner;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.GROUP;
    }

    @Override
    public String toReadableString() {
        return "(" + inner.toReadableString() + ")";
    }

    @Override
    public ValidationResult validate() {
        return inner.validate();
    }

    @Override
    public List<String> getParameters() {
        return inner.getParameters();
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitGroup(this);
    }

    @Override
    public Expression copy() {
        return new GroupExpression(inner.copy());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupExpression that = (GroupExpression) o;
        return inner.equals(that.inner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner);
    }

    @Override
    public String toString() {
        return toReadableString();
    }

    /**
     * Creates a grouped expression.
     */
    public static GroupExpression of(Expression inner) {
        return new GroupExpression(inner);
    }
}
