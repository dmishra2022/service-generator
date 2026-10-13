package com.enterprise.generator.core.spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Validates every field of a {@link GenerationRequest.Builder} and emits a single
 * {@link ValidationException} containing every detected problem.
 *
 * <p>Validation covers:
 * <ol>
 *   <li>Null and blank checks on all required strings.</li>
 *   <li>Path syntax (via {@link Paths#get}).</li>
 *   <li>Source directory existence and type (must be a directory, must be readable, must contain
 *       a {@code pom.xml}).</li>
 *   <li>Target directory sanity (must not equal source, must not be inside source, must either be
 *       absent or empty unless {@code --force} is set, parent must be writable).</li>
 *   <li>Context-name grammar via {@link ContextName#of(String)}.</li>
 *   <li>Cross-field invariant: new context differs from old context.</li>
 * </ol>
 *
 * <p>The validator never performs destructive operations.
 */
public final class GenerationRequestValidator {

    private GenerationRequestValidator() {
    }

    static GenerationRequest validateAndBuild(GenerationRequest.Builder builder) {
        List<ValidationException.FieldError> errors = new ArrayList<>();

        Path source = parsePath("source", builder.rawSource(), errors);
        Path target = parsePath("target", builder.rawTarget(), errors);

        ContextName newContext = parseContext("newContext", builder.rawNewContext(), errors);
        ContextName oldContext = parseContext("oldContext", builder.rawOldContext(), errors);

        if (source != null) {
            validateSource(source, errors);
        }
        if (target != null) {
            validateTarget(target, source, builder.rawForce(), errors);
        }
        if (newContext != null && oldContext != null && newContext.equals(oldContext)) {
            errors.add(new ValidationException.FieldError(
                    "newContext",
                    "must differ from oldContext (both are '" + newContext.asString() + "')"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // Normalize paths to absolute canonical form.
        return new GenerationRequest(
                source.toAbsolutePath().normalize(),
                target.toAbsolutePath().normalize(),
                newContext,
                oldContext,
                builder.rawForce(),
                builder.rawDryRun());
    }

    private static Path parsePath(String field, String raw, List<ValidationException.FieldError> errors) {
        if (raw == null) {
            errors.add(new ValidationException.FieldError(field, "must not be null"));
            return null;
        }
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) {
            errors.add(new ValidationException.FieldError(field, "must not be blank"));
            return null;
        }
        try {
            return Paths.get(trimmed);
        } catch (InvalidPathException e) {
            errors.add(new ValidationException.FieldError(field, "is not a valid path: " + e.getReason()));
            return null;
        }
    }

    private static ContextName parseContext(String field, String raw, List<ValidationException.FieldError> errors) {
        try {
            return ContextName.of(raw);
        } catch (ValidationException e) {
            // Re-attribute the inner errors to the correct field name on the request.
            for (ValidationException.FieldError inner : e.errors()) {
                errors.add(new ValidationException.FieldError(field, inner.message()));
            }
            return null;
        }
    }

    private static void validateSource(Path source, List<ValidationException.FieldError> errors) {
        if (!Files.exists(source)) {
            errors.add(new ValidationException.FieldError("source", "directory does not exist: " + source));
            return;
        }
        if (!Files.isDirectory(source)) {
            errors.add(new ValidationException.FieldError("source", "is not a directory: " + source));
            return;
        }
        if (!Files.isReadable(source)) {
            errors.add(new ValidationException.FieldError("source", "is not readable: " + source));
            return;
        }
        Path pom = source.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            errors.add(new ValidationException.FieldError(
                    "source",
                    "does not look like a Maven project (missing pom.xml at " + pom + ")"));
        }
    }

    private static void validateTarget(
            Path target,
            Path source,
            boolean force,
            List<ValidationException.FieldError> errors) {
        Path absTarget;
        try {
            absTarget = target.toAbsolutePath().normalize();
        } catch (Exception e) {
            errors.add(new ValidationException.FieldError("target", "cannot resolve absolute path: " + e.getMessage()));
            return;
        }

        if (source != null) {
            Path absSource = source.toAbsolutePath().normalize();
            if (absTarget.equals(absSource)) {
                errors.add(new ValidationException.FieldError(
                        "target", "must differ from source (both resolve to " + absTarget + ")"));
                return;
            }
            if (absTarget.startsWith(absSource)) {
                errors.add(new ValidationException.FieldError(
                        "target",
                        "must not be nested inside the source directory (" + absTarget + " is inside " + absSource + ")"));
                return;
            }
            if (absSource.startsWith(absTarget)) {
                errors.add(new ValidationException.FieldError(
                        "target",
                        "must not be a parent of the source directory (" + absTarget + " contains " + absSource + ")"));
                return;
            }
        }

        if (Files.exists(absTarget)) {
            if (!Files.isDirectory(absTarget)) {
                errors.add(new ValidationException.FieldError(
                        "target", "already exists and is not a directory: " + absTarget));
                return;
            }
            if (!isEmpty(absTarget) && !force) {
                errors.add(new ValidationException.FieldError(
                        "target",
                        "already exists and is not empty: " + absTarget + " (pass force=true to overwrite)"));
                return;
            }
        } else {
            Path parent = absTarget.getParent();
            if (parent == null) {
                errors.add(new ValidationException.FieldError(
                        "target", "has no writable parent directory: " + absTarget));
                return;
            }
            if (!Files.exists(parent)) {
                errors.add(new ValidationException.FieldError(
                        "target", "parent directory does not exist: " + parent));
                return;
            }
            if (!Files.isDirectory(parent)) {
                errors.add(new ValidationException.FieldError(
                        "target", "parent is not a directory: " + parent));
                return;
            }
            if (!Files.isWritable(parent)) {
                errors.add(new ValidationException.FieldError(
                        "target", "parent is not writable: " + parent));
            }
        }
    }

    private static boolean isEmpty(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        } catch (IOException e) {
            // If we cannot read the directory, treat it as non-empty so the user has to be
            // explicit about overwriting it.
            return false;
        }
    }
}
