package com.enterprise.generator.core.spec;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The complete, validated input to a generation run.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code source} - absolute path to the template directory to clone from.</li>
 *   <li>{@code target} - absolute path where the new project will be written.</li>
 *   <li>{@code newContext} - the new bounded-context name that replaces {@code order}.</li>
 *   <li>{@code oldContext} - the context name present in the template; defaults to {@code order}.
 *       Exposed so a user can regenerate a service that was itself generated from the template.</li>
 *   <li>{@code force} - if {@code true}, allows overwriting a non-empty target directory.</li>
 *   <li>{@code dryRun} - if {@code true}, no files are written; the engine returns a report of
 *       what would have been written.</li>
 * </ul>
 *
 * <p>Instantiate through {@link Builder}, never directly. The builder runs
 * {@link GenerationRequestValidator} and surfaces every error at once.
 */
public record GenerationRequest(
        Path source,
        Path target,
        ContextName newContext,
        ContextName oldContext,
        boolean force,
        boolean dryRun) {

    public GenerationRequest {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(newContext, "newContext must not be null");
        Objects.requireNonNull(oldContext, "oldContext must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder. All setters accept raw strings; validation happens in {@link #build()} and
     * reports every problem at once instead of failing on the first one.
     */
    public static final class Builder {
        private String source;
        private String target;
        private String newContext;
        private String oldContext = "order";
        private boolean force = false;
        private boolean dryRun = false;

        private Builder() {
        }

        public Builder source(String path) {
            this.source = path;
            return this;
        }

        public Builder source(Path path) {
            this.source = path == null ? null : path.toString();
            return this;
        }

        public Builder target(String path) {
            this.target = path;
            return this;
        }

        public Builder target(Path path) {
            this.target = path == null ? null : path.toString();
            return this;
        }

        public Builder newContext(String name) {
            this.newContext = name;
            return this;
        }

        public Builder oldContext(String name) {
            if (name != null) {
                this.oldContext = name;
            }
            return this;
        }

        public Builder force(boolean value) {
            this.force = value;
            return this;
        }

        public Builder dryRun(boolean value) {
            this.dryRun = value;
            return this;
        }

        /**
         * Validate and build. All field-level errors are collected before any exception is thrown,
         * so a caller only sees {@link ValidationException} once with every problem listed.
         */
        public GenerationRequest build() {
            return GenerationRequestValidator.validateAndBuild(this);
        }

        // Package-private accessors used by the validator so the builder stays self-contained.
        String rawSource() {
            return source;
        }

        String rawTarget() {
            return target;
        }

        String rawNewContext() {
            return newContext;
        }

        String rawOldContext() {
            return oldContext;
        }

        boolean rawForce() {
            return force;
        }

        boolean rawDryRun() {
            return dryRun;
        }
    }
}
