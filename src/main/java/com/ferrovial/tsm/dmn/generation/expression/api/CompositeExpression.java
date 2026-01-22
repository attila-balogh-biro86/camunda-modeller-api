package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a composite expression combining multiple expressions with logical operators.
 *
 * <p>Example:</p>
 * <pre>
 * (amount > 100) AND (status == "active") OR (priority == "high")
 * </pre>
 *
 * <p>Internally stores expressions as a list with associated logical operators.</p>
 */
public class CompositeExpression implements Expression {

    private final List<ExpressionNode> nodes;

    private CompositeExpression(List<ExpressionNode> nodes) {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
    }

    /**
     * Returns the list of expression nodes in this composite.
     */
    public List<ExpressionNode> getNodes() {
        return nodes;
    }

    /**
     * Returns just the expressions (without logical operators).
     */
    public List<Expression> getExpressions() {
        return nodes.stream()
                .map(ExpressionNode::getExpression)
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of expressions in this composite.
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Returns true if this composite is empty.
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.COMPOSITE;
    }

    @Override
    public String toReadableString() {
        if (nodes.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            ExpressionNode node = nodes.get(i);
            if (i > 0 && node.getLogicalOperator() != null) {
                sb.append(" ").append(node.getLogicalOperator().getDisplayName()).append(" ");
            }
            sb.append(node.getExpression().toReadableString());
        }
        return sb.toString();
    }

    @Override
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();

        if (nodes.isEmpty()) {
            result.addWarning("Composite expression is empty");
            return result.build();
        }

        // First node should not have a logical operator
        if (nodes.get(0).getLogicalOperator() != null) {
            result.addWarning("First expression should not have a logical operator");
        }

        // Subsequent nodes must have logical operators
        for (int i = 1; i < nodes.size(); i++) {
            if (nodes.get(i).getLogicalOperator() == null) {
                result.addError("Expression at position " + i + " is missing a logical operator");
            }
        }

        // Validate each expression
        for (ExpressionNode node : nodes) {
            result.merge(node.getExpression().validate());
        }

        return result.build();
    }

    @Override
    public List<String> getParameters() {
        return nodes.stream()
                .flatMap(node -> node.getExpression().getParameters().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitComposite(this);
    }

    @Override
    public Expression copy() {
        List<ExpressionNode> copiedNodes = nodes.stream()
                .map(node -> new ExpressionNode(node.getLogicalOperator(), node.getExpression().copy()))
                .collect(Collectors.toList());
        return new CompositeExpression(copiedNodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositeExpression that = (CompositeExpression) o;
        return nodes.equals(that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }

    @Override
    public String toString() {
        return toReadableString();
    }

    // ========================================================================
    // Static Factory Methods
    // ========================================================================

    /**
     * Creates a composite with expressions joined by AND.
     */
    public static CompositeExpression and(Expression... expressions) {
        return and(List.of(expressions));
    }

    /**
     * Creates a composite with expressions joined by AND.
     */
    public static CompositeExpression and(List<Expression> expressions) {
        List<ExpressionNode> nodes = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            LogicalOperator op = (i == 0) ? null : LogicalOperator.AND;
            nodes.add(new ExpressionNode(op, expressions.get(i)));
        }
        return new CompositeExpression(nodes);
    }

    /**
     * Creates a composite with expressions joined by OR.
     */
    public static CompositeExpression or(Expression... expressions) {
        return or(List.of(expressions));
    }

    /**
     * Creates a composite with expressions joined by OR.
     */
    public static CompositeExpression or(List<Expression> expressions) {
        List<ExpressionNode> nodes = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            LogicalOperator op = (i == 0) ? null : LogicalOperator.OR;
            nodes.add(new ExpressionNode(op, expressions.get(i)));
        }
        return new CompositeExpression(nodes);
    }

    /**
     * Creates a builder for constructing a CompositeExpression.
     */
    public static Builder builder() {
        return new Builder();
    }

    // ========================================================================
    // ExpressionNode - Internal node structure
    // ========================================================================

    /**
     * Represents a node in the composite expression.
     * Each node has an optional logical operator (AND/OR) and an expression.
     */
    public static class ExpressionNode {
        private final LogicalOperator logicalOperator;
        private final Expression expression;

        public ExpressionNode(LogicalOperator logicalOperator, Expression expression) {
            this.logicalOperator = logicalOperator;
            this.expression = Objects.requireNonNull(expression, "Expression cannot be null");
        }

        public LogicalOperator getLogicalOperator() {
            return logicalOperator;
        }

        public Expression getExpression() {
            return expression;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExpressionNode that = (ExpressionNode) o;
            return logicalOperator == that.logicalOperator &&
                    expression.equals(that.expression);
        }

        @Override
        public int hashCode() {
            return Objects.hash(logicalOperator, expression);
        }
    }

    // ========================================================================
    // Builder
    // ========================================================================

    /**
     * Builder for CompositeExpression.
     */
    public static class Builder {
        private final List<ExpressionNode> nodes = new ArrayList<>();

        /**
         * Adds the first expression (no logical operator).
         */
        public Builder first(Expression expression) {
            if (!nodes.isEmpty()) {
                throw new IllegalStateException("First expression already added. Use and() or or().");
            }
            nodes.add(new ExpressionNode(null, expression));
            return this;
        }

        /**
         * Adds an expression with AND.
         */
        public Builder and(Expression expression) {
            if (nodes.isEmpty()) {
                throw new IllegalStateException("Must add first expression before using and()");
            }
            nodes.add(new ExpressionNode(LogicalOperator.AND, expression));
            return this;
        }

        /**
         * Adds an expression with OR.
         */
        public Builder or(Expression expression) {
            if (nodes.isEmpty()) {
                throw new IllegalStateException("Must add first expression before using or()");
            }
            nodes.add(new ExpressionNode(LogicalOperator.OR, expression));
            return this;
        }

        /**
         * Adds an expression with the specified logical operator.
         */
        public Builder add(LogicalOperator operator, Expression expression) {
            nodes.add(new ExpressionNode(operator, expression));
            return this;
        }

        /**
         * Builds the CompositeExpression.
         */
        public CompositeExpression build() {
            return new CompositeExpression(nodes);
        }
    }
}
