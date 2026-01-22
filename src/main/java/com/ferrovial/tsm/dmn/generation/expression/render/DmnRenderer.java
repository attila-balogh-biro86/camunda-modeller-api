package com.ferrovial.tsm.dmn.generation.expression.render;

import com.ferrovial.tsm.dmn.generation.expression.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders expressions to Camunda DMN XML format.
 *
 * <p>This renderer creates a complete DMN decision table with:</p>
 * <ul>
 *   <li>Input columns for each unique parameter</li>
 *   <li>Rules generated from AND/OR logic</li>
 *   <li>FEEL expressions for conditions</li>
 * </ul>
 */
public class DmnRenderer implements ExpressionRenderer<String> {

    private final DmnConfig config;

    public DmnRenderer() {
        this(DmnConfig.defaults());
    }

    public DmnRenderer(DmnConfig config) {
        this.config = config;
    }

    @Override
    public String render(Expression expression) {
        StringBuilder xml = new StringBuilder();

        // XML Header
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\"\n");
        xml.append("             xmlns:dmndi=\"https://www.omg.org/spec/DMN/20191111/DMNDI/\"\n");
        xml.append("             xmlns:dc=\"http://www.omg.org/spec/DMN/20180521/DC/\"\n");
        xml.append("             xmlns:camunda=\"http://camunda.org/schema/1.0/dmn\"\n");
        xml.append("             id=\"").append(config.definitionsId).append("\"\n");
        xml.append("             name=\"").append(config.definitionsName).append("\"\n");
        xml.append("             namespace=\"http://camunda.org/schema/1.0/dmn\">\n\n");

        // Decision
        xml.append("  <decision id=\"").append(config.decisionId).append("\" ");
        xml.append("name=\"").append(config.decisionName).append("\">\n");
        xml.append("    <decisionTable id=\"").append(config.tableId).append("\" ");
        xml.append("hitPolicy=\"").append(config.hitPolicy).append("\">\n\n");

        // Collect all parameters
        List<String> parameters = expression.getParameters();

        // Input columns
        for (int i = 0; i < parameters.size(); i++) {
            String param = parameters.get(i);
            DataType dataType = config.parameterTypes.getOrDefault(param, DataType.STRING);

            xml.append("      <input id=\"input_").append(i + 1).append("\" ");
            xml.append("label=\"").append(param).append("\">\n");
            xml.append("        <inputExpression id=\"inputExpr_").append(i + 1).append("\" ");
            xml.append("typeRef=\"").append(dataType.getDmnTypeRef()).append("\">\n");
            xml.append("          <text>").append(param).append("</text>\n");
            xml.append("        </inputExpression>\n");
            xml.append("      </input>\n\n");
        }

        // Output column
        xml.append("      <output id=\"output_1\" ");
        xml.append("label=\"").append(config.outputName).append("\" ");
        xml.append("name=\"").append(config.outputName).append("\" ");
        xml.append("typeRef=\"").append(config.outputType.getDmnTypeRef()).append("\"/>\n\n");

        // Generate rules
        List<DmnRule> rules = generateRules(expression, parameters);
        for (int i = 0; i < rules.size(); i++) {
            xml.append(renderRule(rules.get(i), i + 1, parameters));
        }

        // Close tags
        xml.append("    </decisionTable>\n");
        xml.append("  </decision>\n");
        xml.append("</definitions>\n");

        return xml.toString();
    }

    @Override
    public String getName() {
        return "DMN";
    }

    @Override
    public String getDescription() {
        return "Camunda DMN 1.3 Decision Table XML";
    }

    // ========================================================================
    // Rule Generation
    // ========================================================================

    private List<DmnRule> generateRules(Expression expression, List<String> parameters) {
        List<DmnRule> rules = new ArrayList<>();

        if (expression instanceof ConstantExpression) {
            // Constant true = match anything
            ConstantExpression constant = (ConstantExpression) expression;
            if (constant.getValue()) {
                DmnRule rule = new DmnRule();
                rule.inputEntries = parameters.stream()
                        .map(p -> "") // Empty = match anything
                        .collect(Collectors.toList());
                rule.outputEntry = config.outputValue;
                rules.add(rule);
            }
        } else if (expression instanceof Condition) {
            rules.add(conditionToRule((Condition) expression, parameters));
        } else if (expression instanceof CompositeExpression) {
            rules.addAll(compositeToRules((CompositeExpression) expression, parameters));
        } else if (expression instanceof GroupExpression) {
            rules.addAll(generateRules(((GroupExpression) expression).getInner(), parameters));
        }

        return rules;
    }

    private DmnRule conditionToRule(Condition condition, List<String> parameters) {
        DmnRule rule = new DmnRule();
        rule.inputEntries = new ArrayList<>();
        rule.outputEntry = config.outputValue;

        for (String param : parameters) {
            if (param.equals(condition.getParameterName())) {
                rule.inputEntries.add(conditionToFeel(condition));
            } else {
                rule.inputEntries.add(""); // Match anything
            }
        }

        return rule;
    }

    private List<DmnRule> compositeToRules(CompositeExpression composite, List<String> parameters) {
        // Group by OR - each OR creates a new rule
        List<List<CompositeExpression.ExpressionNode>> ruleGroups = groupByOr(composite.getNodes());
        List<DmnRule> rules = new ArrayList<>();

        for (List<CompositeExpression.ExpressionNode> group : ruleGroups) {
            DmnRule rule = new DmnRule();
            rule.inputEntries = new ArrayList<>();
            rule.outputEntry = config.outputValue;

            // Collect conditions per parameter
            for (String param : parameters) {
                List<String> feelExprs = new ArrayList<>();

                for (CompositeExpression.ExpressionNode node : group) {
                    Expression expr = node.getExpression();
                    if (expr instanceof Condition) {
                        Condition cond = (Condition) expr;
                        if (cond.getParameterName().equals(param)) {
                            feelExprs.add(conditionToFeel(cond));
                        }
                    }
                }

                if (feelExprs.isEmpty()) {
                    rule.inputEntries.add("");
                } else if (feelExprs.size() == 1) {
                    rule.inputEntries.add(feelExprs.get(0));
                } else {
                    // Multiple conditions on same parameter - combine
                    rule.inputEntries.add(String.join(", ", feelExprs));
                }
            }

            rules.add(rule);
        }

        return rules;
    }

    private List<List<CompositeExpression.ExpressionNode>> groupByOr(
            List<CompositeExpression.ExpressionNode> nodes) {
        List<List<CompositeExpression.ExpressionNode>> groups = new ArrayList<>();
        List<CompositeExpression.ExpressionNode> currentGroup = new ArrayList<>();

        for (CompositeExpression.ExpressionNode node : nodes) {
            if (node.getLogicalOperator() == LogicalOperator.OR && !currentGroup.isEmpty()) {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(node);
        }

        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups;
    }

    private String conditionToFeel(Condition condition) {
        return condition.getOperator().toFeelExpression(
                condition.getValue(),
                condition.getDataType()
        );
    }

    private String renderRule(DmnRule rule, int ruleNum, List<String> parameters) {
        StringBuilder xml = new StringBuilder();
        xml.append("      <rule id=\"rule_").append(ruleNum).append("\">\n");

        // Input entries
        for (int i = 0; i < rule.inputEntries.size(); i++) {
            xml.append("        <inputEntry id=\"inputEntry_").append(ruleNum).append("_").append(i + 1).append("\">\n");
            xml.append("          <text>").append(escapeXml(rule.inputEntries.get(i))).append("</text>\n");
            xml.append("        </inputEntry>\n");
        }

        // Output entry
        xml.append("        <outputEntry id=\"outputEntry_").append(ruleNum).append("\">\n");
        xml.append("          <text>").append(escapeXml(formatOutputValue(rule.outputEntry))).append("</text>\n");
        xml.append("        </outputEntry>\n");
        xml.append("      </rule>\n\n");

        return xml.toString();
    }

    private String formatOutputValue(String value) {
        if (config.outputType == DataType.STRING) {
            return "\"" + value + "\"";
        }
        return value;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ========================================================================
    // Internal Classes
    // ========================================================================

    private static class DmnRule {
        List<String> inputEntries;
        String outputEntry;
    }

    // ========================================================================
    // Configuration
    // ========================================================================

    /**
     * Configuration for DMN rendering.
     */
    public static class DmnConfig {
        public String definitionsId = "definitions_1";
        public String definitionsName = "DRD";
        public String decisionId = "decision_1";
        public String decisionName = "Business Rule Decision";
        public String tableId = "decisionTable_1";
        public String hitPolicy = "FIRST";
        public String outputName = "result";
        public String outputValue = "approved";
        public DataType outputType = DataType.STRING;
        public java.util.Map<String, DataType> parameterTypes = new java.util.HashMap<>();

        public static DmnConfig defaults() {
            return new DmnConfig();
        }

        public DmnConfig definitionsId(String id) {
            this.definitionsId = id;
            return this;
        }

        public DmnConfig decisionId(String id) {
            this.decisionId = id;
            return this;
        }

        public DmnConfig decisionName(String name) {
            this.decisionName = name;
            return this;
        }

        public DmnConfig hitPolicy(String policy) {
            this.hitPolicy = policy;
            return this;
        }

        public DmnConfig output(String name, String value, DataType type) {
            this.outputName = name;
            this.outputValue = value;
            this.outputType = type;
            return this;
        }

        public DmnConfig parameterType(String param, DataType type) {
            this.parameterTypes.put(param, type);
            return this;
        }
    }
}
