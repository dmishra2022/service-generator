package com.enterprise.generator.core.apply;

import java.util.Objects;

/**
 * One line in the generation report. Captures the kind of change, the source and target relative
 * paths, and the number of in-file substitutions performed (zero for binaries and unchanged
 * files).
 */
public record FileChange(Kind kind, String sourceRelative, String targetRelative, int replacements) {

    public FileChange {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(sourceRelative, "sourceRelative must not be null");
        Objects.requireNonNull(targetRelative, "targetRelative must not be null");
        if (replacements < 0) {
            throw new IllegalArgumentException("replacements must not be negative");
        }
    }

    public enum Kind {
        /** A directory was created at the target. */
        DIRECTORY_CREATED,
        /** A binary file was copied byte-for-byte. */
        BINARY_COPIED,
        /** A text file was copied and transformed. */
        TEXT_TRANSFORMED,
        /** A text file was copied with no substitutions (the plan did not touch it). */
        TEXT_UNCHANGED,
        /** The file or directory was skipped by the exclusion policy. */
        SKIPPED,
        /** A path collision was detected but the engine resolved it (dry-run only). */
        RENAMED
    }
}
