package com.enterprise.generator.web.dto;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

/**
 * RFC 7807 problem+json body returned on validation or scan failures.
 */
@Introspected
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        List<FieldErrorView> errors) {

    @Introspected
    public record FieldErrorView(String field, String message) {
    }
}
