package com.enterprise.generator.core.plan;

import java.util.List;
import java.util.Objects;

/**
 * The complete, ordered set of rules the generator will apply during a run.
 *
 * <p>Two independent rule lists live here:
 * <ul>
 *   <li>{@code pathRules} - transform file-tree paths (directory renames).</li>
 *   <li>{@code contentRules} - transform text-file contents (package names, artifact ids, etc).</li>
 * </ul>
 *
 * <p>Both lists are already ordered so that longer, more specific prefixes come before shorter
 * ones. Consumers must apply rules in declared order.
 */
public record RenamingPlan(List<PathRule> pathRules, List<ContentRule> contentRules) {

    public RenamingPlan {
        pathRules = List.copyOf(Objects.requireNonNull(pathRules, "pathRules must not be null"));
        contentRules = List.copyOf(Objects.requireNonNull(contentRules, "contentRules must not be null"));
    }

    /**
     * Apply every path rule in order and return the final transformed path.
     *
     * <p>Path rules carry an explicit trailing slash so that a rule like
     * {@code order-domain/ -> billing-domain/} never matches an unrelated path segment such as
     * {@code order-domain-tests}. To make the rule also match a path that <em>is</em> the module
     * directory itself (relative path {@code "order-domain"} with no trailing slash), we virtually
     * append a slash before applying rules and strip it afterwards.
     */
    public String applyPath(String path) {
        if (path.isEmpty()) {
            return path;
        }
        boolean hadTrailingSlash = path.endsWith("/");
        String current = hadTrailingSlash ? path : path + "/";
        for (PathRule rule : pathRules) {
            current = rule.apply(current);
        }
        if (!hadTrailingSlash && current.endsWith("/")) {
            current = current.substring(0, current.length() - 1);
        }
        return current;
    }
}
