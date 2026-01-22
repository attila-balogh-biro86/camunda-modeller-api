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
}
