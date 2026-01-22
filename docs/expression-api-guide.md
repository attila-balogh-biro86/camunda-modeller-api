# Expression API and DMN Generator Guide

This guide explains how to use the Expression API to build business rule expressions and generate Camunda DMN decision tables programmatically.

## Table of Contents

- [Overview](#overview)
- [Expression API](#expression-api)
  - [Expression Types](#expression-types)
  - [Building Expressions with ExpressionBuilder](#building-expressions-with-expressionbuilder)
  - [Operators](#operators)
  - [Data Types](#data-types)
- [DMN Generation](#dmn-generation)
  - [Single Expression Generation](#single-expression-generation)
  - [Multi-Rule Generation](#multi-rule-generation)
  - [Configuration Options](#configuration-options)
- [Examples](#examples)
  - [Simple Eligibility Check](#simple-eligibility-check)
  - [Complex Business Rules](#complex-business-rules)
  - [Gap Auto-Closing Scenario](#gap-auto-closing-scenario)

---

## Overview

The Expression API provides a framework-agnostic, fluent DSL for building complex business rule expressions. These expressions can be rendered to multiple formats:

- **FEEL** - DMN Friendly Enough Expression Language
- **Java** - Executable Java boolean expressions
- **DMN XML** - Complete Camunda DMN decision tables

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    EXPRESSION API                           │
├─────────────────────────────────────────────────────────────┤
│  ExpressionBuilder (Fluent DSL)                             │
│          ↓                                                  │
│  Expression Interface                                       │
│  ├─ Condition          (single comparison)                  │
│  ├─ CompositeExpression (AND/OR combinations)               │
│  ├─ GroupExpression    (parenthesized)                      │
│  └─ ConstantExpression (always true/false)                  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              ExpressionDmnGeneratorService                  │
├─────────────────────────────────────────────────────────────┤
│  • generateModel() → DmnModelInstance                       │
│  • generateXml() → DMN XML String                           │
│  • generateModelFromRules() → Multi-rule DMN                │
│  Uses Camunda DMN Model API directly                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Expression API

### Expression Types

#### 1. Condition

A single comparison between a parameter and a value.

```java
// Static factory methods
Condition.equals("status", "active");
Condition.greaterThan("amount", "100");
Condition.contains("name", "truck");
Condition.isNull("notes");

// Builder pattern
Condition.builder("age", Operator.GREATER_THAN_OR_EQUAL)
    .value("18")
    .dataType(DataType.INTEGER)
    .build();
```

#### 2. CompositeExpression

Multiple expressions combined with AND/OR operators.

```java
// Static factory methods
CompositeExpression.and(
    Condition.greaterThan("age", "18"),
    Condition.equals("hasLicense", "true")
);

CompositeExpression.or(
    Condition.equals("status", "VIP"),
    Condition.greaterThan("purchases", "1000")
);

// Builder pattern
CompositeExpression.builder()
    .first(Condition.equals("country", "ES"))
    .and(Condition.greaterThan("age", "18"))
    .or(Condition.equals("status", "VIP"))
    .build();
```

#### 3. GroupExpression

Parenthesized expression for controlling operator precedence.

```java
// ((a > 1) AND (b < 2)) OR (c == 3)
GroupExpression.of(
    CompositeExpression.and(
        Condition.greaterThan("a", "1"),
        Condition.lessThan("b", "2")
    )
);
```

#### 4. ConstantExpression

Always-true or always-false expressions for default/catch-all rules.

```java
ConstantExpression.alwaysTrue();   // Matches anything
ConstantExpression.alwaysFalse();  // Matches nothing
ConstantExpression.alwaysTrue("Default Rule");  // With custom label
```

---

### Building Expressions with ExpressionBuilder

The `ExpressionBuilder` provides a fluent API for constructing expressions.

#### Basic Usage

```java
Expression expr = ExpressionBuilder.create()
    .eq("status", "active")
    .and()
    .gt("amount", "100")
    .build();
// Result: (status == "active") AND (amount > 100)
```

#### Shorthand Methods

| Method | Operator | Example |
|--------|----------|---------|
| `eq(param, value)` | `==` | `.eq("status", "active")` |
| `neq(param, value)` | `!=` | `.neq("type", "internal")` |
| `gt(param, value)` | `>` | `.gt("age", "18")` |
| `gte(param, value)` | `>=` | `.gte("score", "60")` |
| `lt(param, value)` | `<` | `.lt("price", "100")` |
| `lte(param, value)` | `<=` | `.lte("quantity", "10")` |
| `contains(param, value)` | contains | `.contains("name", "truck")` |
| `startsWith(param, value)` | starts with | `.startsWith("code", "PRE")` |
| `endsWith(param, value)` | ends with | `.endsWith("email", ".com")` |
| `isNull(param)` | is null | `.isNull("notes")` |
| `isNotNull(param)` | is not null | `.isNotNull("assignee")` |
| `isEmpty(param)` | is empty | `.isEmpty("description")` |
| `in(param, values...)` | in list | `.in("country", "ES", "FR", "DE")` |
| `between(param, min, max)` | range | `.between("age", "18", "65")` |

#### Combining with AND/OR

```java
// AND combination
Expression expr = ExpressionBuilder.create()
    .gte("age", "18")
    .and()
    .eq("hasLicense", "true")
    .and()
    .eq("country", "ES")
    .build();

// OR combination (creates multiple DMN rules)
Expression expr = ExpressionBuilder.create()
    .eq("status", "VIP")
    .or()
    .gt("purchases", "10000")
    .build();

// Mixed AND/OR
Expression expr = ExpressionBuilder.create()
    .eq("type", "premium")
    .and()
    .gte("balance", "1000")
    .or()
    .eq("status", "VIP")
    .build();
```

#### Grouping for Precedence

```java
Expression expr = ExpressionBuilder.create()
    .group(g -> g
        .eq("status", "active")
        .and()
        .gt("age", "18"))
    .or()
    .eq("role", "admin")
    .build();
// Result: ((status == "active") AND (age > 18)) OR (role == "admin")
```

#### Validation

```java
// Build with validation (throws on error)
Expression expr = ExpressionBuilder.create()
    .eq("status", "active")
    .buildAndValidate();

// Manual validation
Expression expr = ExpressionBuilder.create()
    .eq("status", "active")
    .build();

ValidationResult result = expr.validate();
if (!result.isValid()) {
    System.err.println("Errors: " + result.getErrors());
}
```

---

### Operators

| Category | Operator | Symbol | Requires Value | FEEL Output |
|----------|----------|--------|----------------|-------------|
| **Comparison** | EQUALS | `==` | Yes | `"value"` or `value` |
| | NOT_EQUALS | `!=` | Yes | `not("value")` |
| **Numeric** | GREATER_THAN | `>` | Yes | `> 100` |
| | GREATER_THAN_OR_EQUAL | `>=` | Yes | `>= 100` |
| | LESS_THAN | `<` | Yes | `< 100` |
| | LESS_THAN_OR_EQUAL | `<=` | Yes | `<= 100` |
| | BETWEEN | `between` | Yes | `[10..20]` |
| **String** | CONTAINS | `contains` | Yes | `contains(?, "value")` |
| | NOT_CONTAINS | `not contains` | Yes | `not(contains(?, "value"))` |
| | STARTS_WITH | `startsWith` | Yes | `starts with "value"` |
| | ENDS_WITH | `endsWith` | Yes | `ends with "value"` |
| | MATCHES | `matches` | Yes | `matches(?, "regex")` |
| **Null/Empty** | IS_NULL | `isNull` | No | `null` |
| | IS_NOT_NULL | `isNotNull` | No | `not(null)` |
| | IS_EMPTY | `isEmpty` | No | `null` |
| | IS_NOT_EMPTY | `isNotEmpty` | No | `not(null)` |
| **Collection** | IN | `in` | Yes | `"a", "b", "c"` |
| | NOT_IN | `notIn` | Yes | `not("a", "b", "c")` |

---

### Data Types

| DataType | DMN Type Ref | Java Type |
|----------|--------------|-----------|
| `STRING` | `string` | `String.class` |
| `INTEGER` | `integer` | `Integer.class` |
| `LONG` | `long` | `Long.class` |
| `DOUBLE` | `double` | `Double.class` |
| `BOOLEAN` | `boolean` | `Boolean.class` |
| `DATE` | `date` | `LocalDate.class` |
| `DATETIME` | `dateTime` | `LocalDateTime.class` |

Data types are automatically inferred from values, or can be explicitly set:

```java
// Auto-inferred as INTEGER
Condition.greaterThan("age", "18");

// Explicitly set
Condition.builder("age", Operator.GREATER_THAN)
    .value("18")
    .dataType(DataType.INTEGER)
    .build();
```

---

## DMN Generation

### Single Expression Generation

Use `ExpressionDmnGeneratorService` to generate DMN from a single expression.

```java
// Build expression
Expression expr = ExpressionBuilder.create()
    .gte("age", "60")
    .and()
    .eq("hasLicense", "true")
    .build();

// Configure DMN
ExpressionDmnConfig config = ExpressionDmnConfig.builder()
    .decisionId("driver_eligibility")
    .decisionName("Driver Eligibility Check")
    .hitPolicy(HitPolicy.FIRST)
    .parameterType("age", DataType.INTEGER)
    .parameterType("hasLicense", DataType.BOOLEAN)
    .parameterLabel("age", "Driver Age")
    .parameterLabel("hasLicense", "Has Valid License")
    .addOutput("eligible", "Eligible", DataType.BOOLEAN, "false")
    .addOutput("reason", "Reason", DataType.STRING, "Too old to drive")
    .build();

// Generate DMN
String xml = ExpressionDmnGeneratorService.generateXml(expr, config);

// Or get DmnModelInstance for further manipulation
DmnModelInstance model = ExpressionDmnGeneratorService.generateModel(expr, config);
```

### Multi-Rule Generation

For DMN tables with multiple rules having different output values, use `RuleDefinition`:

```java
// Define rules with their expressions and output values
List<ExpressionDmnGeneratorService.RuleDefinition> rules = List.of(
    // Rule 1: age < 18 -> not eligible, "Minor"
    ExpressionDmnGeneratorService.RuleDefinition.of(
        ExpressionBuilder.create().lt("age", "18").build(),
        "false", "\"Minor - cannot drive\""
    ),
    // Rule 2: age >= 18 AND age < 60 -> eligible
    ExpressionDmnGeneratorService.RuleDefinition.of(
        ExpressionBuilder.create()
            .gte("age", "18")
            .and()
            .lt("age", "60")
            .build(),
        "true", "\"Eligible to drive\""
    ),
    // Rule 3: age >= 60 -> not eligible, "Senior"
    ExpressionDmnGeneratorService.RuleDefinition.of(
        ExpressionBuilder.create().gte("age", "60").build(),
        "false", "\"Senior - restricted driving\""
    ),
    // Rule 4: Default catch-all
    ExpressionDmnGeneratorService.RuleDefinition.of(
        ConstantExpression.alwaysTrue(),
        "false", "\"Unknown status\""
    )
);

// Configure and generate
ExpressionDmnConfig config = ExpressionDmnConfig.builder()
    .decisionId("age_check")
    .decisionName("Age Eligibility Check")
    .hitPolicy(HitPolicy.FIRST)
    .parameterType("age", DataType.INTEGER)
    .addOutput("eligible", "Eligible", DataType.BOOLEAN, "")
    .addOutput("reason", "Reason", DataType.STRING, "")
    .build();

String xml = ExpressionDmnGeneratorService.generateXmlFromRules(rules, config);
```

### Configuration Options

#### ExpressionDmnConfig Builder Methods

| Method | Description | Default |
|--------|-------------|---------|
| `definitionsId(String)` | DMN definitions element ID | `"definitions_1"` |
| `definitionsName(String)` | DMN definitions name | `"DRD"` |
| `decisionId(String)` | Decision element ID | `"decision_1"` |
| `decisionName(String)` | Decision display name | `"Business Rule Decision"` |
| `tableId(String)` | Decision table ID | `"decisionTable_1"` |
| `hitPolicy(HitPolicy)` | Hit policy | `HitPolicy.FIRST` |
| `parameterType(name, type)` | Set input column data type | `DataType.STRING` |
| `parameterLabel(name, label)` | Set input column label | Parameter name |
| `addOutput(name, label, type, default)` | Add output column | - |
| `addInput(name, label, expr, type)` | Add computed input column | - |

#### Computed Input Columns

For complex input expressions (like boolean conditions):

```java
ExpressionDmnConfig config = ExpressionDmnConfig.builder()
    .decisionId("gap_closing")
    // Simple input
    .parameterType("gantryStatus", DataType.STRING)
    // Computed boolean input
    .addInput(
        "exceedsMaxGap",                           // Parameter name
        "Exceeds Max Gap",                         // Label
        "gapLength > maxGapLength",                // FEEL expression
        DataType.BOOLEAN                           // Result type
    )
    .build();
```

---

## Examples

### Simple Eligibility Check

```java
// Expression: age >= 18 AND hasLicense == true
Expression expr = ExpressionBuilder.create()
    .gte("age", "18")
    .and()
    .eq("hasLicense", "true")
    .build();

ExpressionDmnConfig config = ExpressionDmnConfig.builder()
    .decisionId("eligibility")
    .decisionName("Eligibility Check")
    .parameterType("age", DataType.INTEGER)
    .parameterType("hasLicense", DataType.BOOLEAN)
    .addOutput("eligible", "Eligible", DataType.BOOLEAN, "true")
    .build();

String dmn = ExpressionDmnGeneratorService.generateXml(expr, config);
```

### Complex Business Rules

```java
// Expression: (status == "VIP" OR purchases > 10000) AND country IN ("ES", "FR", "DE")
Expression expr = ExpressionBuilder.create()
    .group(g -> g
        .eq("status", "VIP")
        .or()
        .gt("purchases", "10000"))
    .and()
    .in("country", "ES", "FR", "DE")
    .build();

ExpressionDmnConfig config = ExpressionDmnConfig.builder()
    .decisionId("discount_eligibility")
    .decisionName("Premium Discount Eligibility")
    .hitPolicy(HitPolicy.FIRST)
    .parameterType("purchases", DataType.INTEGER)
    .addOutput("discount", "Discount %", DataType.INTEGER, "20")
    .build();

String dmn = ExpressionDmnGeneratorService.generateXml(expr, config);
```

### Gap Auto-Closing Scenario

A complete example with multiple rules and different outputs:

```java
// Rule 1: CLOSED and gap exceeds max -> OPEN with zero rate
Expression rule1 = ExpressionBuilder.create()
    .eq("gantryStatus", "CLOSED")
    .and()
    .eq("exceedsMaxGap", "true")
    .build();

// Rule 2: CLOSED and gap within max -> keep CLOSED
Expression rule2 = ExpressionBuilder.create()
    .eq("gantryStatus", "CLOSED")
    .and()
    .eq("exceedsMaxGap", "false")
    .build();

// Rule 3: OPEN -> keep OPEN
Expression rule3 = ExpressionBuilder.create()
    .eq("gantryStatus", "OPEN")
    .build();

// Rule 4: Default safeguard
Expression rule4 = ConstantExpression.alwaysTrue();

// Create rule definitions with output values
List<ExpressionDmnGeneratorService.RuleDefinition> rules = List.of(
    ExpressionDmnGeneratorService.RuleDefinition.of(rule1,
        "\"OPEN\"", "0", "\"Open gantry with zero rate\""),
    ExpressionDmnGeneratorService.RuleDefinition.of(rule2,
        "\"CLOSED\"", "null", "\"Keep gantry closed\""),
    ExpressionDmnGeneratorService.RuleDefinition.of(rule3,
        "\"OPEN\"", "null", "\"Gantry is open\""),
    ExpressionDmnGeneratorService.RuleDefinition.of(rule4,
        "\"OPEN\"", "null", "\"Default safeguard\"")
);

// Configure
ExpressionDmnConfig config = ExpressionDmnConfig.builder()
    .decisionId("automatic_gap_closing")
    .decisionName("Automatic Gap Closing")
    .hitPolicy(HitPolicy.FIRST)
    .parameterType("gantryStatus", DataType.STRING)
    .addInput("exceedsMaxGap", "Exceeds Max Gap",
        "gapLength > maxGapLength", DataType.BOOLEAN)
    .addOutput("action", "Action", DataType.STRING, "")
    .addOutput("rate", "Rate", DataType.LONG, "")
    .addOutput("description", "Description", DataType.STRING, "")
    .build();

// Generate
String dmn = ExpressionDmnGeneratorService.generateXmlFromRules(rules, config);
```

---

## Package Structure

```
com.ferrovial.tsm.dmn.generation
├── expression
│   ├── api
│   │   ├── Expression.java           # Base interface
│   │   ├── Condition.java            # Single condition
│   │   ├── CompositeExpression.java  # AND/OR combinations
│   │   ├── GroupExpression.java      # Parenthesized expressions
│   │   ├── ConstantExpression.java   # Always true/false
│   │   ├── ExpressionBuilder.java    # Fluent builder
│   │   ├── Operator.java             # Comparison operators
│   │   ├── LogicalOperator.java      # AND/OR
│   │   ├── DataType.java             # Data types
│   │   ├── ExpressionType.java       # Expression type enum
│   │   ├── ExpressionVisitor.java    # Visitor pattern
│   │   ├── ValidationResult.java     # Validation results
│   │   └── Parameter.java            # Parameter metadata
│   └── render
│       ├── ExpressionRenderer.java   # Renderer interface
│       ├── FeelRenderer.java         # FEEL output
│       ├── JavaRenderer.java         # Java output
│       └── DmnRenderer.java          # DMN XML output
└── service
    ├── ExpressionDmnGeneratorService.java  # Main service
    ├── ExpressionDmnConfig.java            # Configuration
    └── DmnGeneratorService.java            # JSON-based service
```

---

## See Also

- [Camunda DMN Reference](https://docs.camunda.org/manual/latest/reference/dmn/)
- [FEEL Expression Language](https://docs.camunda.org/manual/latest/reference/dmn/feel/)
