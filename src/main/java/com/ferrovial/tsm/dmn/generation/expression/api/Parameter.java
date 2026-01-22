package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.Objects;

/**
 * Represents a parameter (variable) that can be used in expressions.
 * Parameters have a name, data type, and optional metadata.
 */
public class Parameter {

    private final String name;
    private final DataType dataType;
    private final String displayName;
    private final String description;

    private Parameter(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Parameter name cannot be null");
        this.dataType = builder.dataType != null ? builder.dataType : DataType.STRING;
        this.displayName = builder.displayName != null ? builder.displayName : builder.name;
        this.description = builder.description;
    }

    /**
     * Returns the parameter name (used in expressions).
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the data type of this parameter.
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Returns the display name (for UI).
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the description (optional).
     */
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return name.equals(parameter.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " (" + dataType + ")";
    }

    /**
     * Creates a parameter with just a name (defaults to STRING type).
     */
    public static Parameter of(String name) {
        return builder(name).build();
    }

    /**
     * Creates a parameter with a name and data type.
     */
    public static Parameter of(String name, DataType dataType) {
        return builder(name).dataType(dataType).build();
    }

    /**
     * Creates a builder for constructing a Parameter.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Builder for Parameter.
     */
    public static class Builder {
        private final String name;
        private DataType dataType;
        private String displayName;
        private String description;

        private Builder(String name) {
            this.name = name;
        }

        public Builder dataType(DataType dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Parameter build() {
            return new Parameter(this);
        }
    }
}
