package com.ferrovial.tsm.dmn.generation.expression.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the result of validating an expression.
 */
public class ValidationResult {

    private final List<String> errors;
    private final List<String> warnings;

    private ValidationResult(List<String> errors, List<String> warnings) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /**
     * Returns true if the expression is valid (no errors).
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Returns the list of validation errors.
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns the list of validation warnings.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Returns true if there are any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    @Override
    public String toString() {
        if (isValid() && !hasWarnings()) {
            return "Valid";
        }
        StringBuilder sb = new StringBuilder();
        if (!errors.isEmpty()) {
            sb.append("Errors: ").append(errors);
        }
        if (!warnings.isEmpty()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("Warnings: ").append(warnings);
        }
        return sb.toString();
    }

    /**
     * Creates a valid result with no errors or warnings.
     */
    public static ValidationResult valid() {
        return new ValidationResult(Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Creates a builder for constructing ValidationResult.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ValidationResult.
     */
    public static class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Builder addError(String error) {
            errors.add(error);
            return this;
        }

        public Builder addWarning(String warning) {
            warnings.add(warning);
            return this;
        }

        public Builder merge(ValidationResult other) {
            errors.addAll(other.getErrors());
            warnings.addAll(other.getWarnings());
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(errors, warnings);
        }
    }
}
