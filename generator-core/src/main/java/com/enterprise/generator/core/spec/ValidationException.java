package com.enterprise.generator.core.spec;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when a {@link GenerationRequest} or one of its component value objects fails validation.
 * Carries structured field errors so callers (CLI, HTTP handler) can render precise messages.
 */
public class ValidationException extends RuntimeException {

    private final List<FieldError> errors;

    public ValidationException(String field, String message) {
        this(List.of(new FieldError(field, message)));
    }

    public ValidationException(List<FieldError> errors) {
        super(summarize(errors));
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }

    private static String summarize(List<FieldError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }
        if (errors.size() == 1) {
            return errors.get(0).toString();
        }
        StringBuilder sb = new StringBuilder("Validation failed with ").append(errors.size()).append(" errors:");
        for (FieldError e : errors) {
            sb.append("\n  - ").append(e);
        }
        return sb.toString();
    }

    /** @return the complete list of field-level errors. */
    public List<FieldError> errors() {
        return errors;
    }

    /**
     * A single validation failure. Field names are dotted paths (for example {@code "context"} or
     * {@code "targetDirectory"}).
     */
    public record FieldError(String field, String message) {
        public FieldError {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }

        @Override
        public String toString() {
            return field + ": " + message;
        }
    }
}
