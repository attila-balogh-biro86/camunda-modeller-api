package com.ferrovial.tsm.dmn.generation.expression.render;

import com.ferrovial.tsm.dmn.generation.expression.api.*;

/**
 * Renders expressions to Java boolean expressions.
 *
 * <p>Example output:</p>
 * <pre>
 * (amount > 100) && (status.equals("active"))
 * </pre>
 */
public class JavaRenderer implements ExpressionRenderer<String> {

    private final String variablePrefix;

    public JavaRenderer() {
        this("");
    }

    public JavaRenderer(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public String render(Expression expression) {
        return renderExpression(expression);
    }

    @Override
    public String getName() {
        return "Java";
    }

    @Override
    public String getDescription() {
        return "Java boolean expression";
    }

    private String renderExpression(Expression expression) {
        if (expression instanceof Condition) {
            return renderCondition((Condition) expression);
        } else if (expression instanceof CompositeExpression) {
            return renderComposite((CompositeExpression) expression);
        } else if (expression instanceof GroupExpression) {
            return "(" + renderExpression(((GroupExpression) expression).getInner()) + ")";
        } else if (expression instanceof ConstantExpression) {
            return String.valueOf(((ConstantExpression) expression).getValue());
        }
        return "true";
    }

    private String renderCondition(Condition condition) {
        String param = variablePrefix + condition.getParameterName();
        String value = condition.getValue();
        Operator op = condition.getOperator();
        DataType dataType = condition.getDataType();

        StringBuilder sb = new StringBuilder();
        if (condition.isGrouped()) sb.append("(");

        switch (op) {
            case EQUALS:
                if (dataType == DataType.STRING) {
                    sb.append(param).append(".equals(\"").append(value).append("\")");
                } else {
                    sb.append(param).append(" == ").append(value);
                }
                break;

            case NOT_EQUALS:
                if (dataType == DataType.STRING) {
                    sb.append("!").append(param).append(".equals(\"").append(value).append("\")");
                } else {
                    sb.append(param).append(" != ").append(value);
                }
                break;

            case GREATER_THAN:
                sb.append(param).append(" > ").append(value);
                break;

            case GREATER_THAN_OR_EQUAL:
                sb.append(param).append(" >= ").append(value);
                break;

            case LESS_THAN:
                sb.append(param).append(" < ").append(value);
                break;

            case LESS_THAN_OR_EQUAL:
                sb.append(param).append(" <= ").append(value);
                break;

            case CONTAINS:
                sb.append(param).append(".contains(\"").append(value).append("\")");
                break;

            case NOT_CONTAINS:
                sb.append("!").append(param).append(".contains(\"").append(value).append("\")");
                break;

            case STARTS_WITH:
                sb.append(param).append(".startsWith(\"").append(value).append("\")");
                break;

            case ENDS_WITH:
                sb.append(param).append(".endsWith(\"").append(value).append("\")");
                break;

            case MATCHES:
                sb.append(param).append(".matches(\"").append(value).append("\")");
                break;

            case EQUALS_IGNORE_CASE:
                sb.append(param).append(".equalsIgnoreCase(\"").append(value).append("\")");
                break;

            case IS_NULL:
                sb.append(param).append(" == null");
                break;

            case IS_NOT_NULL:
                sb.append(param).append(" != null");
                break;

            case IS_EMPTY:
                sb.append(param).append(".isEmpty()");
                break;

            case IS_NOT_EMPTY:
                sb.append("!").append(param).append(".isEmpty()");
                break;

            case IN:
                String[] values = value.split(",");
                sb.append("java.util.Arrays.asList(");
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(", ");
                    if (dataType == DataType.STRING) {
                        sb.append("\"").append(values[i].trim()).append("\"");
                    } else {
                        sb.append(values[i].trim());
                    }
                }
                sb.append(").contains(").append(param).append(")");
                break;

            case NOT_IN:
                String[] notInValues = value.split(",");
                sb.append("!java.util.Arrays.asList(");
                for (int i = 0; i < notInValues.length; i++) {
                    if (i > 0) sb.append(", ");
                    if (dataType == DataType.STRING) {
                        sb.append("\"").append(notInValues[i].trim()).append("\"");
                    } else {
                        sb.append(notInValues[i].trim());
                    }
                }
                sb.append(").contains(").append(param).append(")");
                break;

            case BETWEEN:
                String[] range = value.split(",");
                if (range.length == 2) {
                    sb.append("(").append(param).append(" >= ").append(range[0].trim());
                    sb.append(" && ").append(param).append(" <= ").append(range[1].trim()).append(")");
                }
                break;

            default:
                sb.append(param).append(" == ").append(value);
        }

        if (condition.isGrouped()) sb.append(")");
        return sb.toString();
    }

    private String renderComposite(CompositeExpression composite) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < composite.getNodes().size(); i++) {
            CompositeExpression.ExpressionNode node = composite.getNodes().get(i);

            if (i > 0 && node.getLogicalOperator() != null) {
                sb.append(" ").append(node.getLogicalOperator().getJavaSymbol()).append(" ");
            }

            sb.append(renderExpression(node.getExpression()));
        }

        return sb.toString();
    }
}
