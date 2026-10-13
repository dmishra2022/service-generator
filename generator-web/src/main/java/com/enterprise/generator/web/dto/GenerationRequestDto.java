package com.enterprise.generator.web.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP-facing input DTO. Shape-equivalent to the CLI arguments.
 *
 * <p>Jakarta Validation annotations here cover the obviously bad inputs (null, blank, wrong
 * length). All deeper rules (path safety, context grammar, reserved words, source directory
 * existence, target emptiness) are checked by {@link
 * com.enterprise.generator.core.spec.GenerationRequestValidator} which runs when the controller
 * delegates to the core engine. We keep validation duplication to a minimum by letting the core
 * raise its own structured {@code ValidationException} and the controller translate it to RFC 7807
 * problem+json.
 */
@Introspected
public record GenerationRequestDto(

        @NotBlank @Size(max = 4096) String source,

        @NotBlank @Size(max = 4096) String target,

        @NotBlank @Size(min = 2, max = 32) String newContext,

        @Nullable String oldContext,

        @Nullable Boolean force,

        @Nullable Boolean dryRun) {

    public boolean effectiveForce() {
        return force != null && force;
    }

    public boolean effectiveDryRun() {
        return dryRun != null && dryRun;
    }

    public String effectiveOldContext() {
        return oldContext == null ? "order" : oldContext;
    }
}
