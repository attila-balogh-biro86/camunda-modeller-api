package com.ferrovial.tsm.dmn.generation.service;

import com.ferrovial.tsm.dmn.generation.expression.api.*;
import com.ferrovial.tsm.dmn.generation.expression.api.Expression;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating Camunda DMN decision tables from Expression API objects.
 *
 * <p>This service takes Expression objects (built using the ExpressionBuilder or
 * directly via the Expression API classes) and generates valid Camunda DMN XML
 * using the Camunda Model API.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Converts Expression API objects to DMN decision tables</li>
 *   <li>Uses Camunda DMN Model API for proper model construction</li>
 *   <li>Handles AND/OR logic by creating appropriate rules</li>
 *   <li>Supports multiple output columns</li>
 *   <li>Configurable hit policies, parameter types, and labels</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Expression expr = ExpressionBuilder.create()
 *     .condition("amount", Operator.GREATER_THAN, "100")
 *     .and()
 *     .condition("status", Operator.EQUALS, "active")
 *     .build();
 *
 * ExpressionDmnConfig config = ExpressionDmnConfig.builder()
 *     .decisionId("discount_decision")
 *     .decisionName("Discount Eligibility")
 *     .addOutput("result", "Result", DataType.STRING, "approved")
 *     .parameterType("amount", DataType.INTEGER)
 *     .build();
 *
 * // Get as DmnModelInstance for further manipulation
 * DmnModelInstance model = ExpressionDmnGeneratorService.generateModel(expr, config);
 *
 * // Or get directly as XML string
 * String xml = ExpressionDmnGeneratorService.generateXml(expr, config);
 * }</pre>
 */
public class ExpressionDmnGeneratorService {

    /**
     * Generates a Camunda DmnModelInstance from an Expression.
     *
     * @param expression the expression defining the decision logic
     * @param config     the configuration for the DMN generation
     * @return the generated DmnModelInstance
     */
    public static DmnModelInstance generateModel(Expression expression, ExpressionDmnConfig config) {
        DmnModelInstance modelInstance = Dmn.createEmptyModel();

        // Create Definitions root element
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setNamespace("https://www.omg.org/spec/DMN/20191111/MODEL/");
        definitions.setId(config.getDefinitionsId());
        definitions.setName(config.getDefinitionsName());
        modelInstance.setDocumentElement(definitions);

        // Create Decision element
        Decision decision = modelInstance.newInstance(Decision.class);
        decision.setId(config.getDecisionId());
        decision.setName(config.getDecisionName());
        definitions.addChildElement(decision);

        // Create DecisionTable element
        DecisionTable table = modelInstance.newInstance(DecisionTable.class);
        table.setHitPolicy(config.getHitPolicy());
        table.setId(config.getTableId());
        decision.addChildElement(table);

        // Extract parameters from expression and create Input columns
        List<String> parameters = expression.getParameters();
        createInputColumns(modelInstance, table, parameters, config);

        // Create Output columns
        createOutputColumns(modelInstance, table, config);

        // Generate rules from expression and add to table
        List<DmnRule> dmnRules = generateRules(expression, parameters, config);
        createRules(modelInstance, table, dmnRules, parameters.size(), config.getOutputs().size());

        return modelInstance;
    }

    /**
     * Generates DMN XML string from an Expression.
     *
     * @param expression the expression defining the decision logic
     * @param config     the configuration for the DMN generation
     * @return the generated DMN XML as a string
     */
    public static String generateXml(Expression expression, ExpressionDmnConfig config) {
        DmnModelInstance modelInstance = generateModel(expression, config);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Dmn.writeModelToStream(stream, modelInstance);
        return stream.toString(StandardCharsets.UTF_8);
    }

    /**
     * Generates DMN XML string using default configuration.
     *
     * @param expression the expression defining the decision logic
     * @return the generated DMN XML as a string
     */
    public static String generateXml(Expression expression) {
        return generateXml(expression, ExpressionDmnConfig.defaults());
    }

    // ========================================================================
    // Input/Output Column Creation
    // ========================================================================

    private static void createInputColumns(DmnModelInstance modelInstance, DecisionTable table,
                                           List<String> parameters, ExpressionDmnConfig config) {
        for (int i = 0; i < parameters.size(); i++) {
            String paramName = parameters.get(i);
            DataType dataType = config.getParameterType(paramName);
            String label = config.getParameterLabel(paramName);

            Input input = modelInstance.newInstance(Input.class);
            input.setId("input_" + (i + 1));
            input.setLabel(label);

            InputExpression inputExpression = modelInstance.newInstance(InputExpression.class);
            inputExpression.setId("inputExpr_" + (i + 1));
            inputExpression.setTypeRef(dataType.getDmnTypeRef());

            Text text = modelInstance.newInstance(Text.class);
            text.setTextContent(paramName);
            inputExpression.addChildElement(text);

            input.addChildElement(inputExpression);
            table.addChildElement(input);
        }
    }

    private static void createOutputColumns(DmnModelInstance modelInstance, DecisionTable table,
                                            ExpressionDmnConfig config) {
        List<ExpressionDmnConfig.OutputConfig> outputs = config.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            ExpressionDmnConfig.OutputConfig outputConfig = outputs.get(i);

            Output output = modelInstance.newInstance(Output.class);
            output.setId("output_" + (i + 1));
            output.setLabel(outputConfig.getLabel());
            output.setName(outputConfig.getName());
            output.setTypeRef(outputConfig.getDataType().getDmnTypeRef());

            table.addChildElement(output);
        }
    }

    // ========================================================================
    // Rule Creation
    // ========================================================================

    private static void createRules(DmnModelInstance modelInstance, DecisionTable table,
                                    List<DmnRule> dmnRules, int inputCount, int outputCount) {
        for (int r = 0; r < dmnRules.size(); r++) {
            DmnRule dmnRule = dmnRules.get(r);

            Rule rule = modelInstance.newInstance(Rule.class);
            rule.setId("rule_" + (r + 1));

            // Create input entries
            for (int i = 0; i < dmnRule.inputEntries.size(); i++) {
                InputEntry entry = modelInstance.newInstance(InputEntry.class);
                entry.setId("inputEntry_" + (r + 1) + "_" + (i + 1));

                Text text = modelInstance.newInstance(Text.class);
                text.setTextContent(dmnRule.inputEntries.get(i));
                entry.addChildElement(text);

                rule.addChildElement(entry);
            }

            // Create output entries
            for (int i = 0; i < dmnRule.outputEntries.size(); i++) {
                OutputEntry entry = modelInstance.newInstance(OutputEntry.class);
                entry.setId("outputEntry_" + (r + 1) + "_" + (i + 1));

                Text text = modelInstance.newInstance(Text.class);
                text.setTextContent(dmnRule.outputEntries.get(i));
                entry.addChildElement(text);

                rule.addChildElement(entry);
            }

            table.addChildElement(rule);
        }
    }

    // ========================================================================
    // Rule Generation from Expressions
    // ========================================================================

    private static List<DmnRule> generateRules(Expression expression, List<String> parameters,
                                               ExpressionDmnConfig config) {
        List<DmnRule> rules = new ArrayList<>();

        if (expression instanceof ConstantExpression) {
            ConstantExpression constant = (ConstantExpression) expression;
            if (constant.getValue()) {
                // Always true = match anything
                DmnRule rule = new DmnRule();
                rule.inputEntries = parameters.stream()
                        .map(p -> "") // Empty = match anything in DMN
                        .collect(Collectors.toList());
                rule.outputEntries = config.getOutputs().stream()
                        .map(ExpressionDmnConfig.OutputConfig::getFormattedDefaultValue)
                        .collect(Collectors.toList());
                rules.add(rule);
            }
            // Always false = no rules (nothing matches)
        } else if (expression instanceof Condition) {
            rules.add(conditionToRule((Condition) expression, parameters, config));
        } else if (expression instanceof CompositeExpression) {
            rules.addAll(compositeToRules((CompositeExpression) expression, parameters, config));
        } else if (expression instanceof GroupExpression) {
            // Unwrap group and process inner expression
            rules.addAll(generateRules(((GroupExpression) expression).getInner(), parameters, config));
        }

        return rules;
    }

    private static DmnRule conditionToRule(Condition condition, List<String> parameters,
                                           ExpressionDmnConfig config) {
        DmnRule rule = new DmnRule();
        rule.inputEntries = new ArrayList<>();
        rule.outputEntries = config.getOutputs().stream()
                .map(ExpressionDmnConfig.OutputConfig::getFormattedDefaultValue)
                .collect(Collectors.toList());

        for (String param : parameters) {
            if (param.equals(condition.getParameterName())) {
                rule.inputEntries.add(conditionToFeel(condition));
            } else {
                rule.inputEntries.add(""); // Match anything
            }
        }

        return rule;
    }

    private static List<DmnRule> compositeToRules(CompositeExpression composite, List<String> parameters,
                                                  ExpressionDmnConfig config) {
        // Group by OR - each OR creates a new rule
        List<List<CompositeExpression.ExpressionNode>> ruleGroups = groupByOr(composite.getNodes());
        List<DmnRule> rules = new ArrayList<>();

        for (List<CompositeExpression.ExpressionNode> group : ruleGroups) {
            DmnRule rule = new DmnRule();
            rule.inputEntries = new ArrayList<>();
            rule.outputEntries = config.getOutputs().stream()
                    .map(ExpressionDmnConfig.OutputConfig::getFormattedDefaultValue)
                    .collect(Collectors.toList());

            // Collect conditions per parameter
            for (String param : parameters) {
                List<String> feelExprs = new ArrayList<>();

                for (CompositeExpression.ExpressionNode node : group) {
                    Expression expr = node.getExpression();
                    collectConditionsForParameter(expr, param, feelExprs);
                }

                if (feelExprs.isEmpty()) {
                    rule.inputEntries.add("");
                } else if (feelExprs.size() == 1) {
                    rule.inputEntries.add(feelExprs.get(0));
                } else {
                    // Multiple conditions on same parameter - combine with comma (FEEL list)
                    rule.inputEntries.add(String.join(", ", feelExprs));
                }
            }

            rules.add(rule);
        }

        return rules;
    }

    private static void collectConditionsForParameter(Expression expr, String param, List<String> feelExprs) {
        if (expr instanceof Condition) {
            Condition cond = (Condition) expr;
            if (cond.getParameterName().equals(param)) {
                feelExprs.add(conditionToFeel(cond));
            }
        } else if (expr instanceof GroupExpression) {
            // Unwrap and recurse
            collectConditionsForParameter(((GroupExpression) expr).getInner(), param, feelExprs);
        } else if (expr instanceof CompositeExpression) {
            // Handle nested composites (only AND within a rule group)
            for (CompositeExpression.ExpressionNode node : ((CompositeExpression) expr).getNodes()) {
                collectConditionsForParameter(node.getExpression(), param, feelExprs);
            }
        }
    }

    private static List<List<CompositeExpression.ExpressionNode>> groupByOr(
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

    private static String conditionToFeel(Condition condition) {
        return condition.getOperator().toFeelExpression(
                condition.getValue(),
                condition.getDataType()
        );
    }

    // ========================================================================
    // Internal Classes
    // ========================================================================

    /**
     * Internal representation of a DMN rule during generation.
     */
    private static class DmnRule {
        List<String> inputEntries;
        List<String> outputEntries;
    }
}
