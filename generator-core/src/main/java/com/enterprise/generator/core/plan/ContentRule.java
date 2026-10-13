package com.enterprise.generator.core.plan;

import java.util.Objects;

/**
 * A substring replacement that applies to the textual content of a file. The engine applies all
 * content rules in declared order, so rules whose {@code from} is a prefix of another rule must be
 * listed after the longer one.
 */
public record ContentRule(String from, String to, String description) {

    public ContentRule {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (from.isEmpty()) {
            throw new IllegalArgumentException("from must not be empty");
        }
    }

    public ContentRule(String from, String to) {
        this(from, to, from + " -> " + to);
    }

    /**
     * Apply the rule to the given content.
     *
     * @return the transformed content and the number of occurrences that were replaced.
     */
    public Result apply(String content) {
        if (content.isEmpty()) {
            return new Result(content, 0);
        }
        int count = countOccurrences(content, from);
        if (count == 0) {
            return new Result(content, 0);
        }
        return new Result(content.replace(from, to), count);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** The outcome of applying a rule: the new content and the number of replacements made. */
    public record Result(String content, int replacements) {
    }
}
