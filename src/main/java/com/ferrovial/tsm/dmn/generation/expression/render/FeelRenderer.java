package com.ferrovial.tsm.dmn.generation.expression.render;

import com.ferrovial.tsm.dmn.generation.expression.api.*;

/**
 * Renders expressions to FEEL (Friendly Enough Expression Language) format.
 * FEEL is the expression language used in DMN.
 *
 * <p>Example output:</p>
 * <pre>
 * (amount > 100) and (status = "active")
 * </pre>
 */
public class FeelRenderer implements ExpressionRenderer<String> {

    @Override
    public String render(Expression expression) {
        return renderExpression(expression);
    }

    @Override
    public String getName() {
        return "FEEL";
    }

    @Override
    public String getDescription() {
        return "DMN FEEL expression language";
    }

    private String renderExpression(Expression expression) {
        if (expression instanceof Condition) {
            return renderCondition((Condition) expression);
        } else if (expression instanceof CompositeExpression) {
            return renderComposite((CompositeExpression) expression);
        } else if (expression instanceof GroupExpression) {
            return "(" + renderExpression(((GroupExpression) expression).getInner()) + ")";
        } else if (expression instanceof ConstantExpression) {
            return ((ConstantExpression) expression).getValue() ? "true" : "false";
        }
        return "true";
    }

    private String renderCondition(Condition condition) {
        String param = condition.getParameterName();
        String value = condition.getValue();
        Operator op = condition.getOperator();
        DataType dataType = condition.getDataType();

        StringBuilder sb = new StringBuilder();
        if (condition.isGrouped()) sb.append("(");

        sb.append(param).append(" ");

        switch (op) {
            case EQUALS:
                if (dataType == DataType.STRING) {
                    sb.append("= \"").append(value).append("\"");
                } else {
                    sb.append("= ").append(value);
                }
                break;

            case NOT_EQUALS:
                if (dataType == DataType.STRING) {
                    sb.append("!= \"").append(value).append("\"");
                } else {
                    sb.append("!= ").append(value);
                }
                break;

            case GREATER_THAN:
                sb.append("> ").append(value);
                break;

            case GREATER_THAN_OR_EQUAL:
                sb.append(">= ").append(value);
                break;

            case LESS_THAN:
                sb.append("< ").append(value);
                break;

            case LESS_THAN_OR_EQUAL:
                sb.append("<= ").append(value);
                break;

            case CONTAINS:
                sb.setLength(sb.length() - 1); // Remove trailing space
                sb.setLength(sb.length() - param.length()); // Remove param
                sb.append("contains(").append(param).append(", \"").append(value).append("\")");
                break;

            case NOT_CONTAINS:
                sb.setLength(sb.length() - 1);
                sb.setLength(sb.length() - param.length());
                sb.append("not(contains(").append(param).append(", \"").append(value).append("\"))");
                break;

            case STARTS_WITH:
                sb.setLength(sb.length() - 1);
                sb.setLength(sb.length() - param.length());
                sb.append("starts with(").append(param).append(", \"").append(value).append("\")");
                break;

            case ENDS_WITH:
                sb.setLength(sb.length() - 1);
                sb.setLength(sb.length() - param.length());
                sb.append("ends with(").append(param).append(", \"").append(value).append("\")");
                break;

            case MATCHES:
                sb.setLength(sb.length() - 1);
                sb.setLength(sb.length() - param.length());
                sb.append("matches(").append(param).append(", \"").append(value).append("\")");
                break;

            case EQUALS_IGNORE_CASE:
                sb.setLength(sb.length() - 1);
                sb.setLength(sb.length() - param.length());
                sb.append("lower case(").append(param).append(") = \"").append(value.toLowerCase()).append("\"");
                break;

            case IS_NULL:
                sb.append("= null");
                break;

            case IS_NOT_NULL:
                sb.append("!= null");
                break;

            case IS_EMPTY:
                sb.setLength(sb.length() - 1);
                sb.setLength(sb.length() - param.length());
                sb.append(param).append(" = null or ").append(param).append(" = \"\"");
                break;

            case IS_NOT_EMPTY:
                sb.setLength(sb.length() - 1);
                sb.setLength(sb.length() - param.length());
                sb.append(param).append(" != null and ").append(param).append(" != \"\"");
                break;

            case IN:
                String[] values = value.split(",");
                sb.append("in (");
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(", ");
                    if (dataType == DataType.STRING) {
                        sb.append("\"").append(values[i].trim()).append("\"");
                    } else {
                        sb.append(values[i].trim());
                    }
                }
                sb.append(")");
                break;

            case NOT_IN:
                String[] notInValues = value.split(",");
                sb.append("not in (");
                for (int i = 0; i < notInValues.length; i++) {
                    if (i > 0) sb.append(", ");
                    if (dataType == DataType.STRING) {
                        sb.append("\"").append(notInValues[i].trim()).append("\"");
                    } else {
                        sb.append(notInValues[i].trim());
                    }
                }
                sb.append(")");
                break;

            case BETWEEN:
                String[] range = value.split(",");
                if (range.length == 2) {
                    sb.append("in [").append(range[0].trim()).append("..").append(range[1].trim()).append("]");
                }
                break;

            default:
                sb.append("= ").append(value);
        }

        if (condition.isGrouped()) sb.append(")");
        return sb.toString();
    }

    private String renderComposite(CompositeExpression composite) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < composite.getNodes().size(); i++) {
            CompositeExpression.ExpressionNode node = composite.getNodes().get(i);

            if (i > 0 && node.getLogicalOperator() != null) {
                sb.append(" ").append(node.getLogicalOperator().getSymbol()).append(" ");
            }

            sb.append(renderExpression(node.getExpression()));
        }

        return sb.toString();
    }
}
