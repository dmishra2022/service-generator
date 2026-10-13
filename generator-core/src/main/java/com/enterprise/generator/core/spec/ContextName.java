package com.enterprise.generator.core.spec;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The lowercase name of a bounded context, used both as the Java package segment and as the
 * hyphenated prefix of every module directory.
 *
 * <p>Rules:
 * <ul>
 *   <li>Lowercase ASCII letters, digits, and underscores only.</li>
 *   <li>Must start with a letter.</li>
 *   <li>Length between 2 and 32 characters inclusive.</li>
 *   <li>Must not be a Java reserved word (see {@link JavaReservedWords}).</li>
 * </ul>
 *
 * <p>Instances are immutable and interchangeable with the underlying string, but the type carries
 * the proof that the content has been validated.
 */
public final class ContextName {

    /** Visible for tests and for messages: the full regular expression as a string. */
    public static final String PATTERN_SOURCE = "[a-z][a-z0-9_]*";

    /** The minimum length (inclusive). */
    public static final int MIN_LENGTH = 2;

    /** The maximum length (inclusive). */
    public static final int MAX_LENGTH = 32;

    private static final Pattern PATTERN = Pattern.compile(PATTERN_SOURCE);

    private final String value;

    private ContextName(String value) {
        this.value = value;
    }

    /**
     * Parse and validate a context name. The input is trimmed; validation errors are reported with
     * precise messages.
     *
     * @throws ValidationException if the input is null, blank, violates the pattern, exceeds the
     *     length bounds, or is a reserved word.
     */
    public static ContextName of(String raw) {
        if (raw == null) {
            throw new ValidationException("context", "must not be null");
        }
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) {
            throw new ValidationException("context", "must not be blank");
        }
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new ValidationException(
                    "context",
                    "length must be between " + MIN_LENGTH + " and " + MAX_LENGTH
                            + " (got " + trimmed.length() + ")");
        }
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException(
                    "context",
                    "must match " + PATTERN_SOURCE + " (lowercase letters, digits, underscores; "
                            + "must start with a letter)");
        }
        if (JavaReservedWords.isReserved(trimmed)) {
            throw new ValidationException(
                    "context",
                    "'" + trimmed + "' is a Java reserved word and cannot be used as a package segment");
        }
        return new ContextName(trimmed);
    }

    /** @return the string form of the context name. */
    public String asString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextName other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
