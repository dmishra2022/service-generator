package com.enterprise.generator.core.apply;

/**
 * Thrown by {@link TemplateCopier} when the generation run fails for a reason other than
 * validation (I/O errors, template inconsistencies, write failures, and so on).
 */
public class GenerationException extends RuntimeException {

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
