package com.ferrovial.tsm.dmn.generation.expression.api;

/**
 * Defines the data types supported for parameters in expressions.
 */
public enum DataType {

    STRING("string", String.class),
    INTEGER("integer", Integer.class),
    LONG("long", Long.class),
    DOUBLE("double", Double.class),
    BOOLEAN("boolean", Boolean.class),
    DATE("date", java.time.LocalDate.class),
    DATETIME("dateTime", java.time.LocalDateTime.class);

    private final String dmnTypeRef;
    private final Class<?> javaType;

    DataType(String dmnTypeRef, Class<?> javaType) {
        this.dmnTypeRef = dmnTypeRef;
        this.javaType = javaType;
    }

    /**
     * Returns the DMN type reference string.
     */
    public String getDmnTypeRef() {
        return dmnTypeRef;
    }

    /**
     * Returns the corresponding Java class.
     */
    public Class<?> getJavaType() {
        return javaType;
    }

    /**
     * Checks if this is a numeric type.
     */
    public boolean isNumeric() {
        return this == INTEGER || this == LONG || this == DOUBLE;
    }

    /**
     * Infers the data type from a string value.
     */
    public static DataType inferFromValue(String value) {
        if (value == null || value.isEmpty()) {
            return STRING;
        }

        // Try to parse as number
        try {
            if (value.contains(".")) {
                Double.parseDouble(value);
                return DOUBLE;
            } else {
                Long.parseLong(value);
                return INTEGER;
            }
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Check for boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return BOOLEAN;
        }

        // Default to string
        return STRING;
    }

    /**
     * Finds a DataType by its DMN type reference.
     */
    public static DataType fromDmnTypeRef(String typeRef) {
        for (DataType dt : values()) {
            if (dt.dmnTypeRef.equalsIgnoreCase(typeRef)) {
                return dt;
            }
        }
        return STRING; // Default
    }
}
