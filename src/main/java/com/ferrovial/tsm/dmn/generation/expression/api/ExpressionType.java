package com.ferrovial.tsm.dmn.generation.expression.api;

/**
 * Defines the types of expressions supported by the API.
 */
public enum ExpressionType {

    /**
     * A simple condition comparing a parameter to a value.
     * Example: amount > 100
     */
    CONDITION,

    /**
     * A composite expression combining multiple expressions with logical operators.
     * Example: (amount > 100) AND (status == active)
     */
    COMPOSITE,

    /**
     * A grouped expression enclosed in parentheses.
     * Example: ((a > 1) AND (b < 2))
     */
    GROUP,

    /**
     * A constant/literal expression that always evaluates to true or false.
     * Example: "Always" condition
     */
    CONSTANT
}
