package com.ferrovial.tsm.dmn.generation.service;

import com.ferrovial.tsm.dmn.generation.expression.api.DataType;
import org.camunda.bpm.model.dmn.HitPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Expression-based DMN generation using Camunda API.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ExpressionDmnConfig config = ExpressionDmnConfig.builder()
 *     .decisionId("discount_decision")
 *     .decisionName("Discount Eligibility")
 *     .hitPolicy(HitPolicy.FIRST)
 *     .addOutput("result", "Result", DataType.STRING, "approved")
 *     .parameterType("amount", DataType.INTEGER)
 *     .parameterLabel("amount", "Order Amount")
 *     .build();
 * }</pre>
 */
public class ExpressionDmnConfig {

    private final String definitionsId;
    private final String definitionsName;
    private final String decisionId;
    private final String decisionName;
    private final String tableId;
    private final HitPolicy hitPolicy;
    private final List<OutputConfig> outputs;
    private final Map<String, DataType> parameterTypes;
    private final Map<String, String> parameterLabels;
    private final Map<String, InputConfig> additionalInputs;

    private ExpressionDmnConfig(Builder builder) {
        this.definitionsId = builder.definitionsId;
        this.definitionsName = builder.definitionsName;
        this.decisionId = builder.decisionId;
        this.decisionName = builder.decisionName;
        this.tableId = builder.tableId;
        this.hitPolicy = builder.hitPolicy;
        this.outputs = new ArrayList<>(builder.outputs);
        this.parameterTypes = new HashMap<>(builder.parameterTypes);
        this.parameterLabels = new HashMap<>(builder.parameterLabels);
        this.additionalInputs = new HashMap<>(builder.additionalInputs);
    }

    public String getDefinitionsId() {
        return definitionsId;
    }

    public String getDefinitionsName() {
        return definitionsName;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public String getDecisionName() {
        return decisionName;
    }

    public String getTableId() {
        return tableId;
    }

    public HitPolicy getHitPolicy() {
        return hitPolicy;
    }

    public List<OutputConfig> getOutputs() {
        return outputs;
    }

    public Map<String, DataType> getParameterTypes() {
        return parameterTypes;
    }

    public Map<String, String> getParameterLabels() {
        return parameterLabels;
    }

    public DataType getParameterType(String parameterName) {
        return parameterTypes.getOrDefault(parameterName, DataType.STRING);
    }

    public String getParameterLabel(String parameterName) {
        return parameterLabels.getOrDefault(parameterName, parameterName);
    }

    public Map<String, InputConfig> getAdditionalInputs() {
        return additionalInputs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExpressionDmnConfig defaults() {
        return builder().build();
    }

    /**
     * Configuration for an additional input column with a custom expression.
     * Used for computed columns like "gantryStatus = \"CLOSED\" and (gapLength > maxGapLength)".
     */
    public static class InputConfig {
        private final String name;
        private final String label;
        private final String expression;
        private final DataType dataType;

        public InputConfig(String name, String label, String expression, DataType dataType) {
            this.name = name;
            this.label = label;
            this.expression = expression;
            this.dataType = dataType;
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public String getExpression() {
            return expression;
        }

        public DataType getDataType() {
            return dataType;
        }
    }

    /**
     * Configuration for a single output column.
     */
    public static class OutputConfig {
        private final String name;
        private final String label;
        private final DataType dataType;
        private final String defaultValue;

        public OutputConfig(String name, String label, DataType dataType, String defaultValue) {
            this.name = name;
            this.label = label;
            this.dataType = dataType;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public DataType getDataType() {
            return dataType;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getFormattedDefaultValue() {
            if (dataType == DataType.STRING) {
                return "\"" + defaultValue + "\"";
            }
            return defaultValue;
        }
    }

    /**
     * Builder for ExpressionDmnConfig.
     */
    public static class Builder {
        private String definitionsId = "definitions_1";
        private String definitionsName = "DRD";
        private String decisionId = "decision_1";
        private String decisionName = "Business Rule Decision";
        private String tableId = "decisionTable_1";
        private HitPolicy hitPolicy = HitPolicy.FIRST;
        private final List<OutputConfig> outputs = new ArrayList<>();
        private final Map<String, DataType> parameterTypes = new HashMap<>();
        private final Map<String, String> parameterLabels = new HashMap<>();
        private final Map<String, InputConfig> additionalInputs = new HashMap<>();

        public Builder definitionsId(String definitionsId) {
            this.definitionsId = definitionsId;
            return this;
        }

        public Builder definitionsName(String definitionsName) {
            this.definitionsName = definitionsName;
            return this;
        }

        public Builder decisionId(String decisionId) {
            this.decisionId = decisionId;
            return this;
        }

        public Builder decisionName(String decisionName) {
            this.decisionName = decisionName;
            return this;
        }

        public Builder tableId(String tableId) {
            this.tableId = tableId;
            return this;
        }

        public Builder hitPolicy(HitPolicy hitPolicy) {
            this.hitPolicy = hitPolicy;
            return this;
        }

        /**
         * Adds an output column configuration.
         *
         * @param name         the variable name for the output
         * @param label        the display label for the output column
         * @param dataType     the data type of the output
         * @param defaultValue the default value for rules matching this expression
         */
        public Builder addOutput(String name, String label, DataType dataType, String defaultValue) {
            this.outputs.add(new OutputConfig(name, label, dataType, defaultValue));
            return this;
        }

        /**
         * Adds a simple output with the same name and label.
         */
        public Builder addOutput(String name, DataType dataType, String defaultValue) {
            return addOutput(name, name, dataType, defaultValue);
        }

        /**
         * Sets the data type for a parameter (input column).
         */
        public Builder parameterType(String parameterName, DataType dataType) {
            this.parameterTypes.put(parameterName, dataType);
            return this;
        }

        /**
         * Sets the display label for a parameter (input column).
         */
        public Builder parameterLabel(String parameterName, String label) {
            this.parameterLabels.put(parameterName, label);
            return this;
        }

        /**
         * Adds an additional input column with a custom FEEL expression.
         * Used for computed columns like boolean expressions.
         *
         * @param name       the parameter name used in rule conditions
         * @param label      the display label for the column
         * @param expression the FEEL expression for the input (e.g., "gantryStatus = \"CLOSED\"")
         * @param dataType   the data type of the computed result
         */
        public Builder addInput(String name, String label, String expression, DataType dataType) {
            this.additionalInputs.put(name, new InputConfig(name, label, expression, dataType));
            return this;
        }

        public ExpressionDmnConfig build() {
            // Add default output if none specified
            if (outputs.isEmpty()) {
                outputs.add(new OutputConfig("result", "Result", DataType.STRING, "approved"));
            }
            return new ExpressionDmnConfig(this);
        }
    }
}
