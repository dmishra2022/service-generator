package com.enterprise.generator.web.rest;

import com.enterprise.generator.core.apply.GenerationException;
import com.enterprise.generator.core.scan.TemplateScanException;
import com.enterprise.generator.core.spec.ValidationException;
import com.enterprise.generator.web.dto.ProblemDetail;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Translates the three engine exceptions into RFC 7807 responses. Jakarta Validation failures on
 * the DTO itself are left to Micronaut's default handler, which already produces 400.
 */
public final class ExceptionHandlers {

    private ExceptionHandlers() {
    }

    @Singleton
    @Requires(classes = {ValidationException.class, ExceptionHandler.class})
    @Produces("application/problem+json")
    public static class ValidationExceptionHandler
            implements ExceptionHandler<ValidationException, HttpResponse<ProblemDetail>> {

        @Override
        public HttpResponse<ProblemDetail> handle(HttpRequest request, ValidationException ex) {
            List<ProblemDetail.FieldErrorView> errors = ex.errors().stream()
                    .map(e -> new ProblemDetail.FieldErrorView(e.field(), e.message()))
                    .toList();
            ProblemDetail body = new ProblemDetail(
                    "about:blank",
                    "Validation failed",
                    400,
                    ex.getMessage(),
                    errors);
            return HttpResponse.<ProblemDetail>status(io.micronaut.http.HttpStatus.BAD_REQUEST)
                    .body(body)
                    .contentType(MediaType.of("application/problem+json"));
        }
    }

    @Singleton
    @Requires(classes = {TemplateScanException.class, ExceptionHandler.class})
    @Produces("application/problem+json")
    public static class TemplateScanExceptionHandler
            implements ExceptionHandler<TemplateScanException, HttpResponse<ProblemDetail>> {

        @Override
        public HttpResponse<ProblemDetail> handle(HttpRequest request, TemplateScanException ex) {
            ProblemDetail body = new ProblemDetail(
                    "about:blank",
                    "Invalid template",
                    422,
                    ex.getMessage(),
                    List.of());
            return HttpResponse.<ProblemDetail>status(io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(body)
                    .contentType(MediaType.of("application/problem+json"));
        }
    }

    @Singleton
    @Requires(classes = {GenerationException.class, ExceptionHandler.class})
    @Produces("application/problem+json")
    public static class GenerationExceptionHandler
            implements ExceptionHandler<GenerationException, HttpResponse<ProblemDetail>> {

        @Override
        public HttpResponse<ProblemDetail> handle(HttpRequest request, GenerationException ex) {
            ProblemDetail body = new ProblemDetail(
                    "about:blank",
                    "Generation failed",
                    500,
                    ex.getMessage(),
                    List.of());
            return HttpResponse.<ProblemDetail>status(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body)
                    .contentType(MediaType.of("application/problem+json"));
        }
    }
}
