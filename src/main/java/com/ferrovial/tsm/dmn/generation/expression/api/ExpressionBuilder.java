package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for constructing expressions.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Expression expr = ExpressionBuilder.create()
 *     .condition("amount", Operator.GREATER_THAN, "100")
 *     .and()
 *     .condition("status", Operator.EQUALS, "active")
 *     .or()
 *     .group(g -> g
 *         .condition("priority", Operator.EQUALS, "high")
 *         .and()
 *         .condition("urgent", Operator.EQUALS, "true"))
 *     .build();
 * }</pre>
 */
public class ExpressionBuilder {

    private final List<CompositeExpression.ExpressionNode> nodes = new ArrayList<>();
    private LogicalOperator pendingOperator = null;

    private ExpressionBuilder() {
    }

    /**
     * Creates a new ExpressionBuilder.
     */
    public static ExpressionBuilder create() {
        return new ExpressionBuilder();
    }

    /**
     * Starts building with an "Always true" condition.
     */
    public static Expression alwaysTrue() {
        return ConstantExpression.alwaysTrue();
    }

    /**
     * Starts building with an "Always false" condition.
     */
    public static Expression alwaysFalse() {
        return ConstantExpression.alwaysFalse();
    }

    // ========================================================================
    // Condition Methods
    // ========================================================================

    /**
     * Adds a condition: parameter operator value
     */
    public ExpressionBuilder condition(String parameter, Operator operator, String value) {
        Condition condition = Condition.builder(parameter, operator)
                .value(value)
                .grouped(true)
                .build();
        addExpression(condition);
        return this;
    }

    /**
     * Adds a condition with explicit data type.
     */
    public ExpressionBuilder condition(String parameter, Operator operator, String value, DataType dataType) {
        Condition condition = Condition.builder(parameter, operator)
                .value(value)
                .dataType(dataType)
                .grouped(true)
                .build();
        addExpression(condition);
        return this;
    }

    /**
     * Adds a condition without a value (for operators like IS_NULL, IS_EMPTY).
     */
    public ExpressionBuilder condition(String parameter, Operator operator) {
        Condition condition = Condition.builder(parameter, operator)
                .grouped(true)
                .build();
        addExpression(condition);
        return this;
    }

    /**
     * Adds a pre-built condition.
     */
    public ExpressionBuilder condition(Condition condition) {
        addExpression(condition);
        return this;
    }

    // ========================================================================
    // Shorthand Condition Methods
    // ========================================================================

    /**
     * Adds: parameter == value
     */
    public ExpressionBuilder eq(String parameter, String value) {
        return condition(parameter, Operator.EQUALS, value);
    }

    /**
     * Adds: parameter != value
     */
    public ExpressionBuilder neq(String parameter, String value) {
        return condition(parameter, Operator.NOT_EQUALS, value);
    }

    /**
     * Adds: parameter > value
     */
    public ExpressionBuilder gt(String parameter, String value) {
        return condition(parameter, Operator.GREATER_THAN, value);
    }

    /**
     * Adds: parameter >= value
     */
    public ExpressionBuilder gte(String parameter, String value) {
        return condition(parameter, Operator.GREATER_THAN_OR_EQUAL, value);
    }

    /**
     * Adds: parameter < value
     */
    public ExpressionBuilder lt(String parameter, String value) {
        return condition(parameter, Operator.LESS_THAN, value);
    }

    /**
     * Adds: parameter <= value
     */
    public ExpressionBuilder lte(String parameter, String value) {
        return condition(parameter, Operator.LESS_THAN_OR_EQUAL, value);
    }

    /**
     * Adds: parameter contains value
     */
    public ExpressionBuilder contains(String parameter, String value) {
        return condition(parameter, Operator.CONTAINS, value, DataType.STRING);
    }

    /**
     * Adds: parameter startsWith value
     */
    public ExpressionBuilder startsWith(String parameter, String value) {
        return condition(parameter, Operator.STARTS_WITH, value, DataType.STRING);
    }

    /**
     * Adds: parameter endsWith value
     */
    public ExpressionBuilder endsWith(String parameter, String value) {
        return condition(parameter, Operator.ENDS_WITH, value, DataType.STRING);
    }

    /**
     * Adds: parameter isNull
     */
    public ExpressionBuilder isNull(String parameter) {
        return condition(parameter, Operator.IS_NULL);
    }

    /**
     * Adds: parameter isNotNull
     */
    public ExpressionBuilder isNotNull(String parameter) {
        return condition(parameter, Operator.IS_NOT_NULL);
    }

    /**
     * Adds: parameter isEmpty
     */
    public ExpressionBuilder isEmpty(String parameter) {
        return condition(parameter, Operator.IS_EMPTY);
    }

    /**
     * Adds: parameter in (values)
     */
    public ExpressionBuilder in(String parameter, String... values) {
        return condition(parameter, Operator.IN, String.join(",", values));
    }

    /**
     * Adds: parameter between min and max
     */
    public ExpressionBuilder between(String parameter, String min, String max) {
        return condition(parameter, Operator.BETWEEN, min + "," + max);
    }

    // ========================================================================
    // Logical Operators
    // ========================================================================

    /**
     * Sets AND as the operator for the next condition.
     */
    public ExpressionBuilder and() {
        pendingOperator = LogicalOperator.AND;
        return this;
    }

    /**
     * Sets OR as the operator for the next condition.
     */
    public ExpressionBuilder or() {
        pendingOperator = LogicalOperator.OR;
        return this;
    }

    // ========================================================================
    // Grouping
    // ========================================================================

    /**
     * Adds a grouped expression built using a nested builder.
     *
     * @param builderConsumer consumer that builds the inner expression
     */
    public ExpressionBuilder group(java.util.function.Consumer<ExpressionBuilder> builderConsumer) {
        ExpressionBuilder innerBuilder = new ExpressionBuilder();
        builderConsumer.accept(innerBuilder);
        Expression innerExpr = innerBuilder.build();
        addExpression(new GroupExpression(innerExpr));
        return this;
    }

    /**
     * Adds a pre-built grouped expression.
     */
    public ExpressionBuilder group(Expression inner) {
        addExpression(new GroupExpression(inner));
        return this;
    }

    /**
     * Adds a pre-built expression.
     */
    public ExpressionBuilder expression(Expression expr) {
        addExpression(expr);
        return this;
    }

    // ========================================================================
    // Build Methods
    // ========================================================================

    /**
     * Builds the expression.
     *
     * @return the constructed Expression
     */
    public Expression build() {
        if (nodes.isEmpty()) {
            return ConstantExpression.alwaysTrue();
        }

        if (nodes.size() == 1) {
            return nodes.get(0).getExpression();
        }

        return new CompositeExpression.Builder() {{
            for (CompositeExpression.ExpressionNode node : nodes) {
                add(node.getLogicalOperator(), node.getExpression());
            }
        }}.build();
    }

    /**
     * Builds and validates the expression.
     *
     * @return the constructed Expression
     * @throws IllegalStateException if validation fails
     */
    public Expression buildAndValidate() {
        Expression expr = build();
        ValidationResult result = expr.validate();
        if (!result.isValid()) {
            throw new IllegalStateException("Invalid expression: " + result.getErrors());
        }
        return expr;
    }

    // ========================================================================
    // Internal Methods
    // ========================================================================

    private void addExpression(Expression expression) {
        LogicalOperator operator = nodes.isEmpty() ? null : pendingOperator;
        if (!nodes.isEmpty() && operator == null) {
            operator = LogicalOperator.AND; // Default to AND if not specified
        }
        nodes.add(new CompositeExpression.ExpressionNode(operator, expression));
        pendingOperator = null;
    }

    // ========================================================================
    // Static Factory Methods for Common Patterns
    // ========================================================================

    /**
     * Creates an expression with a single condition.
     */
    public static Expression single(String parameter, Operator operator, String value) {
        return create().condition(parameter, operator, value).build();
    }

    /**
     * Creates an expression with two AND conditions.
     */
    public static Expression and(Condition first, Condition second) {
        return CompositeExpression.and(first, second);
    }

    /**
     * Creates an expression with two OR conditions.
     */
    public static Expression or(Condition first, Condition second) {
        return CompositeExpression.or(first, second);
    }
}
