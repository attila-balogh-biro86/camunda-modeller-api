package com.ferrovial.tsm.dmn;

import com.ferrovial.tsm.dmn.generation.expression.api.*;
import com.ferrovial.tsm.dmn.generation.expression.render.DmnRenderer;
import com.ferrovial.tsm.dmn.generation.expression.render.FeelRenderer;
import com.ferrovial.tsm.dmn.generation.expression.render.JavaRenderer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests demonstrating the Expression API.
 * This test class shows how to use the API for building, serializing,
 * and rendering expressions to different formats.
 */
public class ExpressionApiTest {

    // ========================================================================
    // TEST: Simple Condition Building
    // ========================================================================

    @Test
    public void testSimpleCondition() {
        // Build: amount > 100
        Condition condition = Condition.greaterThan("amount", "100");

        Assertions.assertEquals("amount", condition.getParameterName());
        Assertions.assertEquals(Operator.GREATER_THAN, condition.getOperator());
        Assertions.assertEquals("100", condition.getValue());

        String readable = condition.toReadableString();
        System.out.println("Simple condition: " + readable);
        Assertions.assertEquals("(amount > 100)", readable);
    }

    @Test
    public void testConditionStaticFactories() {
        // Test all static factory methods
        Assertions.assertEquals("(a == b)", Condition.equals("a", "b").toReadableString());
        Assertions.assertEquals("(a != b)", Condition.notEquals("a", "b").toReadableString());
        Assertions.assertEquals("(a > 10)", Condition.greaterThan("a", "10").toReadableString());
        Assertions.assertEquals("(a >= 10)", Condition.greaterThanOrEqual("a", "10").toReadableString());
        Assertions.assertEquals("(a < 10)", Condition.lessThan("a", "10").toReadableString());
        Assertions.assertEquals("(a <= 10)", Condition.lessThanOrEqual("a", "10").toReadableString());
        Assertions.assertEquals("(name contains truck)", Condition.contains("name", "truck").toReadableString());
        Assertions.assertEquals("(name startsWith VH)", Condition.startsWith("name", "VH").toReadableString());
        Assertions.assertEquals("(notes isEmpty)", Condition.isEmpty("notes").toReadableString());
        Assertions.assertEquals("(obj isNull)", Condition.isNull("obj").toReadableString());
    }

    // ========================================================================
    // TEST: ExpressionBuilder Fluent API
    // ========================================================================

    @Test
    public void testExpressionBuilderSimple() {
        Expression expr = ExpressionBuilder.create()
                .condition("amount", Operator.GREATER_THAN, "100")
                .build();

        Assertions.assertEquals("(amount > 100)", expr.toReadableString());
    }

    @Test
    public void testExpressionBuilderWithAnd() {
        Expression expr = ExpressionBuilder.create()
                .condition("amount", Operator.GREATER_THAN, "100")
                .and()
                .condition("status", Operator.EQUALS, "active")
                .build();

        String readable = expr.toReadableString();
        System.out.println("AND expression: " + readable);
        Assertions.assertEquals("(amount > 100) AND (status == active)", readable);
    }

    @Test
    public void testExpressionBuilderWithOr() {
        Expression expr = ExpressionBuilder.create()
                .gt("amount", "500")
                .or()
                .eq("customerType", "vip")
                .build();

        String readable = expr.toReadableString();
        System.out.println("OR expression: " + readable);
        Assertions.assertEquals("(amount > 500) OR (customerType == vip)", readable);
    }

    @Test
    public void testExpressionBuilderComplex() {
        // Build: (amount > 100) AND (status == "active") OR (priority == "high")
        Expression expr = ExpressionBuilder.create()
                .gt("amount", "100")
                .and()
                .eq("status", "active")
                .or()
                .eq("priority", "high")
                .build();

        String readable = expr.toReadableString();
        System.out.println("Complex expression: " + readable);
        Assertions.assertTrue(readable.contains("amount > 100"));
        Assertions.assertTrue(readable.contains("status == active"));
        Assertions.assertTrue(readable.contains("priority == high"));
    }

    @Test
    public void testExpressionBuilderShorthandMethods() {
        Expression expr = ExpressionBuilder.create()
                .gt("a", "10")           // greater than
                .and().gte("b", "20")    // greater than or equal
                .and().lt("c", "30")     // less than
                .and().lte("d", "40")    // less than or equal
                .and().eq("e", "50")     // equals
                .and().neq("f", "60")    // not equals
                .build();

        System.out.println("Shorthand methods: " + expr.toReadableString());
        Assertions.assertEquals(6, expr.getParameters().size());
    }

    @Test
    public void testExpressionBuilderStringOperators() {
        Expression expr = ExpressionBuilder.create()
                .contains("name", "truck")
                .and()
                .startsWith("code", "VH")
                .and()
                .endsWith("id", "001")
                .build();

        System.out.println("String operators: " + expr.toReadableString());
        Assertions.assertTrue(expr.toReadableString().contains("contains"));
        Assertions.assertTrue(expr.toReadableString().contains("startsWith"));
        Assertions.assertTrue(expr.toReadableString().contains("endsWith"));
    }

    @Test
    public void testExpressionBuilderNullChecks() {
        Expression expr = ExpressionBuilder.create()
                .isNotNull("requiredField")
                .and()
                .isEmpty("optionalField")
                .build();

        System.out.println("Null checks: " + expr.toReadableString());
        Assertions.assertTrue(expr.toReadableString().contains("isNotNull"));
        Assertions.assertTrue(expr.toReadableString().contains("isEmpty"));
    }

    @Test
    public void testExpressionBuilderInOperator() {
        Expression expr = ExpressionBuilder.create()
                .in("status", "active", "pending", "approved")
                .build();

        System.out.println("IN operator: " + expr.toReadableString());
        Assertions.assertTrue(expr.toReadableString().contains("in"));
    }

    @Test
    public void testExpressionBuilderBetween() {
        Expression expr = ExpressionBuilder.create()
                .between("age", "18", "65")
                .build();

        System.out.println("BETWEEN: " + expr.toReadableString());
        Assertions.assertTrue(expr.toReadableString().contains("between"));
    }

    @Test
    public void testExpressionBuilderGrouping() {
        // Build: ((a > 1) AND (b < 2)) OR (c == 3)
        Expression expr = ExpressionBuilder.create()
                .group(g -> g
                        .gt("a", "1")
                        .and()
                        .lt("b", "2"))
                .or()
                .eq("c", "3")
                .build();

        System.out.println("Grouped expression: " + expr.toReadableString());
        Assertions.assertTrue(expr.toReadableString().contains("(a > 1)"));
    }

    // ========================================================================
    // TEST: Static Factory Methods
    // ========================================================================

    @Test
    public void testCompositeExpressionAnd() {
        Expression expr = CompositeExpression.and(
                Condition.greaterThan("amount", "100"),
                Condition.equals("status", "active"),
                Condition.lessThan("discount", "50")
        );

        System.out.println("CompositeExpression.and(): " + expr.toReadableString());
        Assertions.assertEquals(3, expr.getParameters().size());
    }

    @Test
    public void testCompositeExpressionOr() {
        Expression expr = CompositeExpression.or(
                Condition.greaterThan("amount", "500"),
                Condition.equals("customerType", "vip")
        );

        System.out.println("CompositeExpression.or(): " + expr.toReadableString());
        Assertions.assertTrue(expr.toReadableString().contains("OR"));
    }

    // ========================================================================
    // TEST: Validation
    // ========================================================================

    @Test
    public void testValidation() {
        Expression validExpr = ExpressionBuilder.create()
                .gt("amount", "100")
                .and()
                .eq("status", "active")
                .build();

        ValidationResult result = validExpr.validate();
        Assertions.assertTrue(result.isValid(),"Expression should be valid");
    }

    @Test
    public void testValidationFailure() {
        // Create condition with missing value for operator that requires one
        Condition invalid = Condition.builder("param", Operator.GREATER_THAN)
                .value("")
                .build();

        ValidationResult result = invalid.validate();
        Assertions.assertFalse(result.isValid(),"Expression should be invalid");
        System.out.println("Validation errors: " + result.getErrors());
    }

    // ========================================================================
    // TEST: Serialization
    // ========================================================================

    @Test
    public void testSerializationToBase64() {
        Expression expr = ExpressionBuilder.create()
                .gt("totalAmount", "100")
                .and()
                .eq("vehicleClass", "premium")
                .build();

        String base64 = ExpressionSerializer.toBase64(expr);
        System.out.println("Base64: " + base64);
        Assertions.assertNotNull(base64);
        Assertions.assertFalse(base64.isEmpty());

        // Deserialize
        Expression restored = ExpressionSerializer.fromBase64(base64);
        Assertions.assertEquals(expr.getParameters().size(), restored.getParameters().size());
        System.out.println("Restored: " + restored.toReadableString());
    }

    @Test
    public void testSerializationToCsv() {
        Expression expr = ExpressionBuilder.create()
                .gt("amount", "100")
                .and()
                .eq("status", "active")
                .build();

        String csv = ExpressionSerializer.toCsv(expr);
        System.out.println("CSV: " + csv);
        Assertions.assertTrue(csv.contains("amount"));
        Assertions.assertTrue(csv.contains("<>")); // Row delimiter
    }

    @Test
    public void testSerializationToJson() {
        Expression expr = ExpressionBuilder.create()
                .gt("amount", "100")
                .and()
                .eq("status", "active")
                .build();

        String json = ExpressionSerializer.toJson(expr);
        System.out.println("JSON:\n" + json);
        Assertions.assertTrue(json.contains("\"type\""));
        Assertions.assertTrue(json.contains("\"parameter\""));
    }

    // ========================================================================
    // TEST: Renderers
    // ========================================================================

    @Test
    public void testJavaRenderer() {
        Expression expr = ExpressionBuilder.create()
                .gt("amount", "100")
                .and()
                .eq("status", "active")
                .build();

        JavaRenderer renderer = new JavaRenderer();
        String java = renderer.render(expr);

        System.out.println("Java: " + java);
        Assertions.assertTrue(java.contains("amount > 100"));
        Assertions.assertTrue(java.contains("status.equals(\"active\")"));
        Assertions.assertTrue(java.contains("&&"));
    }

    @Test
    public void testFeelRenderer() {
        Expression expr = ExpressionBuilder.create()
                .gt("amount", "100")
                .and()
                .eq("status", "active")
                .build();

        FeelRenderer renderer = new FeelRenderer();
        String feel = renderer.render(expr);

        System.out.println("FEEL: " + feel);
        Assertions.assertTrue(feel.contains("amount > 100"));
        Assertions.assertTrue(feel.contains("status = \"active\""));
        Assertions.assertTrue(feel.contains("and"));
    }

    @Test
    public void testDmnRenderer() {
        Expression expr = ExpressionBuilder.create()
                .gt("totalAmount", "100")
                .and()
                .eq("vehicleClass", "premium")
                .or()
                .eq("customerType", "vip")
                .build();

        DmnRenderer renderer = new DmnRenderer(
                DmnRenderer.DmnConfig.defaults()
                        .decisionId("discount_decision")
                        .decisionName("Discount Eligibility")
                        .output("eligible", "yes", DataType.STRING)
                        .parameterType("totalAmount", DataType.INTEGER)
                        .parameterType("vehicleClass", DataType.STRING)
                        .parameterType("customerType", DataType.STRING)
        );

        String dmn = renderer.render(expr);
        System.out.println("DMN XML:\n" + dmn);

        Assertions.assertTrue(dmn.contains("<definitions"));
        Assertions.assertTrue(dmn.contains("discount_decision"));
        Assertions.assertTrue(dmn.contains("<input"));
        Assertions.assertTrue(dmn.contains("<output"));
        Assertions.assertTrue(dmn.contains("<rule"));
        Assertions.assertTrue(dmn.contains("totalAmount"));
    }

    // ========================================================================
    // TEST: Complete Workflow Demo
    // ========================================================================

    @Test
    public void testCompleteWorkflowDemo() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPLETE EXPRESSION API WORKFLOW DEMO");
        System.out.println("=".repeat(70) + "\n");

        // Step 1: Build expression using fluent API
        System.out.println("STEP 1: Build expression using fluent API");
        System.out.println("-".repeat(50));

        Expression expr = ExpressionBuilder.create()
                .gt("totalTripTransactions", "10")
                .and()
                .eq("vehicleClass", "premium")
                .and()
                .gte("tollAmount", "500")
                .or()
                .eq("customerType", "vip")
                .build();

        System.out.println("Expression: " + expr.toReadableString());
        System.out.println("Parameters: " + expr.getParameters());
        System.out.println();

        // Step 2: Validate
        System.out.println("STEP 2: Validate expression");
        System.out.println("-".repeat(50));

        ValidationResult validation = expr.validate();
        System.out.println("Valid: " + validation.isValid());
        System.out.println();

        // Step 3: Serialize to different formats
        System.out.println("STEP 3: Serialize to different formats");
        System.out.println("-".repeat(50));

        System.out.println("CSV:");
        System.out.println("  " + ExpressionSerializer.toCsv(expr));
        System.out.println();

        System.out.println("Base64:");
        String base64 = ExpressionSerializer.toBase64(expr);
        System.out.println("  " + base64);
        System.out.println();

        // Step 4: Render to Java
        System.out.println("STEP 4: Render to Java code");
        System.out.println("-".repeat(50));

        JavaRenderer javaRenderer = new JavaRenderer();
        System.out.println(javaRenderer.render(expr));
        System.out.println();

        // Step 5: Render to FEEL
        System.out.println("STEP 5: Render to FEEL");
        System.out.println("-".repeat(50));

        FeelRenderer feelRenderer = new FeelRenderer();
        System.out.println(feelRenderer.render(expr));
        System.out.println();

        // Step 6: Render to DMN XML
        System.out.println("STEP 6: Render to Camunda DMN XML");
        System.out.println("-".repeat(50));

        DmnRenderer dmnRenderer = new DmnRenderer(
                DmnRenderer.DmnConfig.defaults()
                        .decisionId("discount_eligibility")
                        .decisionName("Discount Eligibility Decision")
                        .hitPolicy("FIRST")
                        .output("discountApproved", "yes", DataType.STRING)
                        .parameterType("totalTripTransactions", DataType.INTEGER)
                        .parameterType("vehicleClass", DataType.STRING)
                        .parameterType("tollAmount", DataType.INTEGER)
                        .parameterType("customerType", DataType.STRING)
        );

        System.out.println(dmnRenderer.render(expr));

        // Step 7: Restore from Base64
        System.out.println("STEP 7: Restore from Base64");
        System.out.println("-".repeat(50));

        Expression restored = ExpressionSerializer.fromBase64(base64);
        System.out.println("Restored: " + restored.toReadableString());
        System.out.println();

        System.out.println("=".repeat(70));
        System.out.println("DEMO COMPLETE");
        System.out.println("=".repeat(70));

        // Assertions
        Assertions.assertTrue(validation.isValid());
        Assertions.assertEquals(4, expr.getParameters().size());
    }

    // ========================================================================
    // TEST: Constant Expressions
    // ========================================================================

    @Test
    public void testConstantExpressions() {
        Expression alwaysTrue = ConstantExpression.alwaysTrue();
        Expression alwaysFalse = ConstantExpression.alwaysFalse();

        Assertions.assertEquals("Always", alwaysTrue.toReadableString());
        Assertions.assertEquals("Never", alwaysFalse.toReadableString());
        Assertions.assertTrue(alwaysTrue.getParameters().isEmpty());
    }

    // ========================================================================
    // TEST: Copy/Clone
    // ========================================================================

    @Test
    public void testExpressionCopy() {
        Expression original = ExpressionBuilder.create()
                .gt("amount", "100")
                .and()
                .eq("status", "active")
                .build();

        Expression copy = original.copy();

        Assertions.assertEquals(original.toReadableString(), copy.toReadableString());
        Assertions.assertNotSame(original, copy);
    }
}
