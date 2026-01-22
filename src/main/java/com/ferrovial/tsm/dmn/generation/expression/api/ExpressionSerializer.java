package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializes and deserializes expressions to various formats.
 *
 * <p>Supported formats:</p>
 * <ul>
 *   <li>Base64 (compatible with existing Vaadin criteria format)</li>
 *   <li>JSON</li>
 *   <li>CSV</li>
 * </ul>
 */
public class ExpressionSerializer {

    private static final String ROW_DELIMITER = "<>";
    private static final String FIELD_DELIMITER = ",";

    private ExpressionSerializer() {
    }

    // ========================================================================
    // Base64 Format (Compatible with existing CriteriaComponent format)
    // ========================================================================

    /**
     * Serializes an expression to Base64 format.
     * Compatible with the existing Vaadin CriteriaComponent format.
     *
     * @param expression the expression to serialize
     * @return Base64 encoded string
     */
    public static String toBase64(Expression expression) {
        String csv = toCsv(expression);
        return Base64.getEncoder().encodeToString(csv.getBytes());
    }

    /**
     * Deserializes an expression from Base64 format.
     *
     * @param base64 the Base64 encoded string
     * @return the deserialized Expression
     */
    public static Expression fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return ConstantExpression.alwaysTrue();
        }

        String csv = new String(Base64.getDecoder().decode(base64));
        return fromCsv(csv);
    }

    // ========================================================================
    // CSV Format
    // ========================================================================

    /**
     * Serializes an expression to CSV format.
     * Format: logicalOp,openParen,param,operator,value,closeParen
     * Multiple rows joined with "<>"
     *
     * @param expression the expression to serialize
     * @return CSV string
     */
    public static String toCsv(Expression expression) {
        List<CsvRow> rows = expressionToRows(expression);
        return rows.stream()
                .map(CsvRow::toCsv)
                .collect(Collectors.joining(ROW_DELIMITER));
    }

    /**
     * Deserializes an expression from CSV format.
     *
     * @param csv the CSV string
     * @return the deserialized Expression
     */
    public static Expression fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return ConstantExpression.alwaysTrue();
        }

        List<String> rowStrings = Arrays.asList(csv.split(ROW_DELIMITER));
        List<CsvRow> rows = rowStrings.stream()
                .map(CsvRow::fromCsv)
                .filter(row -> row != null && !row.parameter.isEmpty())
                .collect(Collectors.toList());

        if (rows.isEmpty()) {
            return ConstantExpression.alwaysTrue();
        }

        return rowsToExpression(rows);
    }

    // ========================================================================
    // Internal - Expression to Rows Conversion
    // ========================================================================

    private static List<CsvRow> expressionToRows(Expression expression) {
        List<CsvRow> rows = new ArrayList<>();

        if (expression instanceof Condition) {
            rows.add(conditionToRow(null, (Condition) expression));
        } else if (expression instanceof CompositeExpression) {
            CompositeExpression composite = (CompositeExpression) expression;
            for (CompositeExpression.ExpressionNode node : composite.getNodes()) {
                rows.addAll(expressionNodeToRows(node));
            }
        } else if (expression instanceof GroupExpression) {
            GroupExpression group = (GroupExpression) expression;
            rows.addAll(expressionToRows(group.getInner()));
        } else if (expression instanceof ConstantExpression) {
            // Constant expressions serialize as empty row
            rows.add(new CsvRow("", "(", "", "", "", ")"));
        }

        return rows;
    }

    private static List<CsvRow> expressionNodeToRows(CompositeExpression.ExpressionNode node) {
        List<CsvRow> rows = new ArrayList<>();
        Expression expr = node.getExpression();

        if (expr instanceof Condition) {
            rows.add(conditionToRow(node.getLogicalOperator(), (Condition) expr));
        } else if (expr instanceof GroupExpression) {
            GroupExpression group = (GroupExpression) expr;
            List<CsvRow> innerRows = expressionToRows(group.getInner());

            // Add extra parentheses to first and last rows
            if (!innerRows.isEmpty()) {
                CsvRow first = innerRows.get(0);
                first.openParen = "(" + first.openParen;

                if (node.getLogicalOperator() != null) {
                    first.logicalOp = node.getLogicalOperator().getSymbol();
                }

                CsvRow last = innerRows.get(innerRows.size() - 1);
                last.closeParen = last.closeParen + ")";

                rows.addAll(innerRows);
            }
        } else if (expr instanceof CompositeExpression) {
            // Nested composite - recursively process
            CompositeExpression nested = (CompositeExpression) expr;
            boolean isFirst = true;
            for (CompositeExpression.ExpressionNode nestedNode : nested.getNodes()) {
                List<CsvRow> nestedRows = expressionNodeToRows(nestedNode);
                if (isFirst && node.getLogicalOperator() != null && !nestedRows.isEmpty()) {
                    nestedRows.get(0).logicalOp = node.getLogicalOperator().getSymbol();
                }
                rows.addAll(nestedRows);
                isFirst = false;
            }
        }

        return rows;
    }

    private static CsvRow conditionToRow(LogicalOperator logicalOp, Condition condition) {
        return new CsvRow(
                logicalOp != null ? logicalOp.getSymbol() : "",
                condition.isGrouped() ? "(" : " ",
                condition.getParameterName(),
                condition.getOperator().getSymbol(),
                condition.getValue() != null ? condition.getValue() : "",
                condition.isGrouped() ? ")" : " "
        );
    }

    // ========================================================================
    // Internal - Rows to Expression Conversion
    // ========================================================================

    private static Expression rowsToExpression(List<CsvRow> rows) {
        if (rows.size() == 1) {
            return rowToCondition(rows.get(0));
        }

        CompositeExpression.Builder builder = CompositeExpression.builder();
        for (int i = 0; i < rows.size(); i++) {
            CsvRow row = rows.get(i);
            Condition condition = rowToCondition(row);

            if (i == 0) {
                builder.first(condition);
            } else {
                LogicalOperator op = LogicalOperator.fromSymbol(row.logicalOp);
                if (op == null) op = LogicalOperator.AND;

                if (op == LogicalOperator.AND) {
                    builder.and(condition);
                } else {
                    builder.or(condition);
                }
            }
        }

        return builder.build();
    }

    private static Condition rowToCondition(CsvRow row) {
        Operator operator;
        try {
            operator = Operator.fromSymbol(row.operator);
        } catch (IllegalArgumentException e) {
            operator = Operator.EQUALS;
        }

        boolean grouped = "(".equals(row.openParen.trim()) || row.openParen.contains("(");

        return Condition.builder(row.parameter, operator)
                .value(row.value)
                .grouped(grouped)
                .build();
    }

    // ========================================================================
    // CSV Row Data Structure
    // ========================================================================

    private static class CsvRow {
        String logicalOp;
        String openParen;
        String parameter;
        String operator;
        String value;
        String closeParen;

        CsvRow(String logicalOp, String openParen, String parameter,
               String operator, String value, String closeParen) {
            this.logicalOp = logicalOp;
            this.openParen = openParen;
            this.parameter = parameter;
            this.operator = operator;
            this.value = value;
            this.closeParen = closeParen;
        }

        String toCsv() {
            return String.join(FIELD_DELIMITER,
                    logicalOp, openParen, parameter, operator, value, closeParen);
        }

        static CsvRow fromCsv(String csv) {
            String[] parts = csv.split(FIELD_DELIMITER, -1);
            if (parts.length < 6) {
                return null;
            }
            return new CsvRow(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4],
                    parts[5]
            );
        }
    }

    // ========================================================================
    // JSON Format
    // ========================================================================

    /**
     * Serializes an expression to JSON format.
     *
     * @param expression the expression to serialize
     * @return JSON string
     */
    public static String toJson(Expression expression) {
        return expressionToJson(expression, 0);
    }

    private static String expressionToJson(Expression expression, int indent) {
        String ind = "  ".repeat(indent);
        String ind2 = "  ".repeat(indent + 1);

        if (expression instanceof Condition) {
            Condition c = (Condition) expression;
            return ind + "{\n" +
                    ind2 + "\"type\": \"condition\",\n" +
                    ind2 + "\"parameter\": \"" + c.getParameterName() + "\",\n" +
                    ind2 + "\"operator\": \"" + c.getOperator().getSymbol() + "\",\n" +
                    ind2 + "\"value\": \"" + (c.getValue() != null ? c.getValue() : "") + "\",\n" +
                    ind2 + "\"dataType\": \"" + c.getDataType() + "\"\n" +
                    ind + "}";
        } else if (expression instanceof CompositeExpression) {
            CompositeExpression composite = (CompositeExpression) expression;
            StringBuilder sb = new StringBuilder();
            sb.append(ind).append("{\n");
            sb.append(ind2).append("\"type\": \"composite\",\n");
            sb.append(ind2).append("\"expressions\": [\n");

            List<CompositeExpression.ExpressionNode> nodes = composite.getNodes();
            for (int i = 0; i < nodes.size(); i++) {
                CompositeExpression.ExpressionNode node = nodes.get(i);
                sb.append(ind2).append("  {\n");
                sb.append(ind2).append("    \"logicalOperator\": ")
                        .append(node.getLogicalOperator() != null ?
                                "\"" + node.getLogicalOperator().getSymbol() + "\"" : "null")
                        .append(",\n");
                sb.append(ind2).append("    \"expression\": ");
                sb.append(expressionToJson(node.getExpression(), indent + 3).trim());
                sb.append("\n").append(ind2).append("  }");
                if (i < nodes.size() - 1) sb.append(",");
                sb.append("\n");
            }

            sb.append(ind2).append("]\n");
            sb.append(ind).append("}");
            return sb.toString();
        } else if (expression instanceof ConstantExpression) {
            ConstantExpression c = (ConstantExpression) expression;
            return ind + "{\n" +
                    ind2 + "\"type\": \"constant\",\n" +
                    ind2 + "\"value\": " + c.getValue() + ",\n" +
                    ind2 + "\"label\": \"" + c.getLabel() + "\"\n" +
                    ind + "}";
        }

        return ind + "{}";
    }
}
