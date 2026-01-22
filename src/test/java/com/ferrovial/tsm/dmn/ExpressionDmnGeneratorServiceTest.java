package com.ferrovial.tsm.dmn;

import com.ferrovial.tsm.dmn.generation.expression.api.*;
import com.ferrovial.tsm.dmn.generation.service.ExpressionDmnConfig;
import com.ferrovial.tsm.dmn.generation.service.ExpressionDmnGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExpressionDmnGeneratorService.
 *
 * These tests validate that the Expression API can be used to generate
 * the same DMN decision tables as the JSON-based DmnGeneratorService,
 * and that the generated DMN executes correctly.
 */
@Slf4j
@SpringBootTest
public class ExpressionDmnGeneratorServiceTest {

    @Autowired
    private DmnEngine dmnEngine;

    /**
     * Tests the same driver eligibility scenario as DmnGenerationTests.testDriverEligibilityWorksOk()
     * but using the Expression API to build the DMN.
     *
     * Test case: Driver aged 61 with license in ES should NOT be eligible
     * because they are too old (>= 60).
     */
    @Test
    public void testDriverEligibilityWithExpressionApi() {
        // Build the expression for rule 3: age >= 60 AND hasLicense == true
        // This rule matches any country (no country condition)
        // Using ExpressionBuilder fluent API
        Expression rule3Expression = ExpressionBuilder.create()
                .gte("age", "60")
                .and()
                .eq("hasLicense", "true")
                .build();

        // Configure the DMN generation
        ExpressionDmnConfig config = ExpressionDmnConfig.builder()
                .decisionId("driver_eligibility_expr")
                .decisionName("Driver Eligibility (Expression API)")
                .hitPolicy(HitPolicy.FIRST)
                .parameterType("age", DataType.INTEGER)
                .parameterType("hasLicense", DataType.BOOLEAN)
                .parameterLabel("age", "Age")
                .parameterLabel("hasLicense", "Has License")
                .addOutput("eligible", "Eligible", DataType.BOOLEAN, "false")
                .addOutput("explanation", "Explanation", DataType.STRING, "Driver is too old to drive regardless its country!")
                .build();

        // Generate the DMN
        String generatedDmn = ExpressionDmnGeneratorService.generateXml(rule3Expression, config);
        assertNotNull(generatedDmn);
        log.debug("Generated DMN:\n{}", generatedDmn);

        // Execute the DMN with test variables (same as DmnGenerationTests)
        Map<String, Object> vars = new HashMap<>();
        vars.put("age", 61);
        vars.put("hasLicense", true);

        DmnDecision decision = dmnEngine.parseDecision("driver_eligibility_expr",
                new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        // Validate results (same assertions as DmnGenerationTests)
        Boolean eligible = result.getSingleResult().getEntry("eligible");
        String explanation = result.getSingleResult().getEntry("explanation");

        log.debug("Is driver eligible: {} - Explanation: {}", eligible, explanation);

        assertNotNull(eligible);
        assertFalse(eligible);
        assertNotNull(explanation);
        assertEquals("Driver is too old to drive regardless its country!", explanation);
    }

    /**
     * Tests a complete driver eligibility scenario with multiple rules using OR logic.
     * This demonstrates how OR expressions create multiple DMN rules.
     */
    @Test
    public void testDriverEligibilityMultipleRulesWithOr() {
        // Build expression: (age < 18) OR (age >= 60)
        // Both conditions should result in "not eligible"
        Expression notEligibleExpression = CompositeExpression.or(
                Condition.lessThan("age", "18"),
                Condition.greaterThanOrEqual("age", "60")
        );

        ExpressionDmnConfig config = ExpressionDmnConfig.builder()
                .decisionId("driver_age_check")
                .decisionName("Driver Age Check")
                .hitPolicy(HitPolicy.FIRST)
                .parameterType("age", DataType.INTEGER)
                .parameterLabel("age", "Driver Age")
                .addOutput("eligible", "Eligible", DataType.BOOLEAN, "false")
                .build();

        String generatedDmn = ExpressionDmnGeneratorService.generateXml(notEligibleExpression, config);
        assertNotNull(generatedDmn);
        log.debug("Generated DMN with OR:\n{}", generatedDmn);

        // Test with age 61 (matches >= 60 rule)
        Map<String, Object> vars = new HashMap<>();
        vars.put("age", 61);

        DmnDecision decision = dmnEngine.parseDecision("driver_age_check",
                new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        Boolean eligible = result.getSingleResult().getEntry("eligible");
        assertNotNull(eligible);
        assertFalse(eligible);

        // Test with age 16 (matches < 18 rule)
        vars.put("age", 16);
        result = dmnEngine.evaluateDecision(decision, vars);
        eligible = result.getSingleResult().getEntry("eligible");
        assertNotNull(eligible);
        assertFalse(eligible);
    }

    /**
     * Tests using the ExpressionBuilder fluent API to construct expressions.
     */
    @Test
    public void testWithExpressionBuilder() {
        // Use the fluent builder API
        Expression expr = ExpressionBuilder.create()
                .gt("age", "60")
                .and()
                .eq("hasLicense", "true")
                .build();

        ExpressionDmnConfig config = ExpressionDmnConfig.builder()
                .decisionId("builder_test")
                .decisionName("Builder Test")
                .parameterType("age", DataType.INTEGER)
                .parameterType("hasLicense", DataType.BOOLEAN)
                .addOutput("result", "Result", DataType.STRING, "senior_driver")
                .build();

        String generatedDmn = ExpressionDmnGeneratorService.generateXml(expr, config);
        assertNotNull(generatedDmn);
        log.debug("Generated DMN with Builder:\n{}", generatedDmn);

        // Execute and validate
        Map<String, Object> vars = new HashMap<>();
        vars.put("age", 65);
        vars.put("hasLicense", true);

        DmnDecision decision = dmnEngine.parseDecision("builder_test",
                new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        String resultValue = result.getSingleResult().getEntry("result");
        assertEquals("senior_driver", resultValue);
    }

    /**
     * Tests that ConstantExpression.TRUE creates a catch-all rule.
     */
    @Test
    public void testConstantExpressionCatchAll() {
        Expression alwaysTrue = ConstantExpression.alwaysTrue();

        ExpressionDmnConfig config = ExpressionDmnConfig.builder()
                .decisionId("catch_all_test")
                .decisionName("Catch All Test")
                .addOutput("status", "Status", DataType.STRING, "default")
                .build();

        String generatedDmn = ExpressionDmnGeneratorService.generateXml(alwaysTrue, config);
        assertNotNull(generatedDmn);
        log.debug("Generated DMN with ConstantExpression:\n{}", generatedDmn);

        // Should match any input
        Map<String, Object> vars = new HashMap<>();

        DmnDecision decision = dmnEngine.parseDecision("catch_all_test",
                new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        String status = result.getSingleResult().getEntry("status");
        assertEquals("default", status);
    }

    /**
     * Tests the same automatic gap closing scenario as DmnGenerationTests.testAutomaticClosingGapRuleGenerationWorksOk()
     * but using the Expression API with multiple rules having different output values.
     *
     * This demonstrates the full power of the Expression API for complex DMN tables with:
     * - Multiple rules with different outputs
     * - Computed input columns (boolean expressions)
     * - Custom FEEL functions in outputs
     *
     * Test case: gantryStatus="OPEN", gapLength=0, maxGapLength=2
     * Expected: action="OPEN", rate=null, description="Gantry is open, no auto closing"
     */
    @Test
    public void testAutomaticGapClosingWithExpressionApi() {
        // Rule 1: CLOSED and gapLength > maxGapLength -> OPEN gantry with zero rate
        Expression rule1 = ExpressionBuilder.create()
                .eq("gantryStatus", "CLOSED")
                .and()
                .eq("closedAndExceedsMax", "true")
                .build();

        // Rule 2: CLOSED and gapLength <= maxGapLength -> keep CLOSED
        Expression rule2 = ExpressionBuilder.create()
                .eq("gantryStatus", "CLOSED")
                .and()
                .eq("closedAndExceedsMax", "false")
                .build();

        // Rule 3: OPEN -> keep OPEN (matches test case)
        Expression rule3 = ExpressionBuilder.create()
                .eq("gantryStatus", "OPEN")
                .build();

        // Rule 4: Default safeguard (catch-all)
        Expression rule4 = ConstantExpression.alwaysTrue();

        // Create rule definitions with their respective output values
        // Output order: action, rate, description
        List<ExpressionDmnGeneratorService.RuleDefinition> rules = List.of(
                ExpressionDmnGeneratorService.RuleDefinition.of(rule1,
                        "executeGantryOperation(\"OPEN_GANTRY\")", "0", "\"Open gantry with zero rate\""),
                ExpressionDmnGeneratorService.RuleDefinition.of(rule2,
                        "executeGantryOperation(\"CLOSE_GANTRY\")", "null", "\"Keep gantry closed, waiting for next rate\""),
                ExpressionDmnGeneratorService.RuleDefinition.of(rule3,
                        "executeGantryOperation(\"OPEN_GANTRY\")", "null", "\"Gantry is open, no auto closing\""),
                ExpressionDmnGeneratorService.RuleDefinition.of(rule4,
                        "executeGantryOperation(\"OPEN_GANTRY\")", "null", "\"Default safeguard\"")
        );

        // Configure the DMN with inputs and outputs matching test_gap_auto_closing.json
        ExpressionDmnConfig config = ExpressionDmnConfig.builder()
                .decisionId("automatic_gap_closing")
                .decisionName("Automatic Gap Closing")
                .hitPolicy(HitPolicy.FIRST)
                // Simple input columns
                .parameterType("gantryStatus", DataType.STRING)
                .parameterLabel("gantryStatus", "input_gantryStatus")
                // Computed boolean input column
                .addInput("closedAndExceedsMax", "input_closed_and_within_max_gap_length",
                        "gantryStatus = \"CLOSED\" and (gapLength > maxGapLength)", DataType.BOOLEAN)
                // Output columns (no default values needed since each rule has its own)
                .addOutput("action", "gap_action", DataType.STRING, "")
                .addOutput("rate", "output_rate", DataType.LONG, "")
                .addOutput("description", "output_description", DataType.STRING, "")
                .build();

        // Generate the DMN
        String generatedDmn = ExpressionDmnGeneratorService.generateXmlFromRules(rules, config);
        assertNotNull(generatedDmn);
        log.debug("Generated Gap Auto Closing DMN:\n{}", generatedDmn);

        // Execute with same test variables as DmnGenerationTests.testAutomaticClosingGapRuleGenerationWorksOk
        Map<String, Object> vars = new HashMap<>();
        vars.put("maxGapLength", 2);
        vars.put("gantryStatus", "OPEN");
        vars.put("gapLength", 0);

        DmnDecision decision = dmnEngine.parseDecision("automatic_gap_closing",
                new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        // Validate results (same assertions as DmnGenerationTests)
        String gantryAction = result.getSingleResult().getEntry("action");
        Long rate = result.getSingleResult().getEntry("rate");
        String description = result.getSingleResult().getEntry("description");

        log.debug("Gantry action: {}", gantryAction);
        log.debug("New rate: {}", rate);
        log.debug("Description: {}", description);

        assertEquals("OPEN", gantryAction);
        assertNull(rate);
        assertEquals("Gantry is open, no auto closing", description);
    }

    /**
     * Tests a simplified version of gap closing rules using ExpressionBuilder
     * without the custom FEEL functions for easier validation.
     */
    @Test
    public void testGapClosingSimplifiedWithExpressionBuilder() {
        // Rule 1: gantryStatus == "CLOSED" -> close action
        Expression closedRule = ExpressionBuilder.create()
                .eq("gantryStatus", "CLOSED")
                .build();

        // Rule 2: gantryStatus == "OPEN" -> open action
        Expression openRule = ExpressionBuilder.create()
                .eq("gantryStatus", "OPEN")
                .build();

        // Rule 3: Default
        Expression defaultRule = ConstantExpression.alwaysTrue();

        List<ExpressionDmnGeneratorService.RuleDefinition> rules = List.of(
                ExpressionDmnGeneratorService.RuleDefinition.of(closedRule, "\"CLOSED\"", "\"Gantry is closed\""),
                ExpressionDmnGeneratorService.RuleDefinition.of(openRule, "\"OPEN\"", "\"Gantry is open\""),
                ExpressionDmnGeneratorService.RuleDefinition.of(defaultRule, "\"UNKNOWN\"", "\"Default state\"")
        );

        ExpressionDmnConfig config = ExpressionDmnConfig.builder()
                .decisionId("gap_closing_simple")
                .decisionName("Gap Closing Simplified")
                .hitPolicy(HitPolicy.FIRST)
                .parameterType("gantryStatus", DataType.STRING)
                .parameterLabel("gantryStatus", "Gantry Status")
                .addOutput("action", "Action", DataType.STRING, "")
                .addOutput("description", "Description", DataType.STRING, "")
                .build();

        String generatedDmn = ExpressionDmnGeneratorService.generateXmlFromRules(rules, config);
        assertNotNull(generatedDmn);
        log.debug("Generated Simplified Gap Closing DMN:\n{}", generatedDmn);

        // Test with OPEN status
        Map<String, Object> vars = new HashMap<>();
        vars.put("gantryStatus", "OPEN");

        DmnDecision decision = dmnEngine.parseDecision("gap_closing_simple",
                new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        String action = result.getSingleResult().getEntry("action");
        String description = result.getSingleResult().getEntry("description");

        assertEquals("OPEN", action);
        assertEquals("Gantry is open", description);

        // Test with CLOSED status
        vars.put("gantryStatus", "CLOSED");
        result = dmnEngine.evaluateDecision(decision, vars);

        action = result.getSingleResult().getEntry("action");
        description = result.getSingleResult().getEntry("description");

        assertEquals("CLOSED", action);
        assertEquals("Gantry is closed", description);
    }
}
