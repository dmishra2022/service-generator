package com.enterprise.generator.core.plan;

import java.util.Objects;

/**
 * A substring replacement that applies to a relative path. The engine applies all path rules in
 * declared order, substring-by-substring, with no regex semantics. Rules work on normalized
 * forward-slash separated paths.
 */
public record PathRule(String from, String to) {

    public PathRule {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from.isEmpty()) {
            throw new IllegalArgumentException("from must not be empty");
        }
    }

    /** @return {@code path} with every occurrence of {@code from} replaced by {@code to}. */
    public String apply(String path) {
        if (path.isEmpty()) {
            return path;
        }
        return path.replace(from, to);
    }
}
