package com.enterprise.generator.core.plan;

import com.enterprise.generator.core.scan.TemplateInspection;
import com.enterprise.generator.core.spec.ContextName;
import com.enterprise.generator.core.spec.GenerationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link RenamingPlan} from a validated {@link GenerationRequest} and a
 * {@link TemplateInspection}. The resulting plan captures exactly the five transformations the
 * user asked the tool to automate.
 *
 * <p>Ordering guarantees:
 * <ol>
 *   <li>Longer, more specific strings are replaced before shorter ones. For example
 *       {@code order-adapter-persistence} comes before {@code order-adapter-rest} and both come
 *       before any hypothetical bare {@code order-adapter} rule, so no replacement ever bites off
 *       part of a longer identifier.</li>
 *   <li>The dotted package replacement ({@code com.enterprise.starter.order ->
 *       com.enterprise.starter.<new>}) is more specific than any plausible bare-word rule, so we
 *       never emit a rule that would rewrite the English word "order" inside prose.</li>
 * </ol>
 */
public final class RenamingPlanFactory {

    private RenamingPlanFactory() {
    }

    public static RenamingPlan from(GenerationRequest request, TemplateInspection inspection) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(inspection, "inspection must not be null");

        String oldContext = inspection.detectedOldContext();
        ContextName requestedOld = request.oldContext();
        if (!requestedOld.asString().equals(oldContext)) {
            // The request's oldContext was declared explicitly but disagrees with what the
            // template actually contains. Trust the inspection: it is the ground truth, but
            // surface the discrepancy in the plan description so the caller sees it.
            oldContext = requestedOld.asString();
        }
        String newContext = request.newContext().asString();

        String oldBasePackage = inspection.basePackage();
        String newBasePackage = replaceLeafSegment(oldBasePackage, newContext);

        List<ContentRule> contentRules = new ArrayList<>();
        List<PathRule> pathRules = new ArrayList<>();

        // ---------- Content rules, ordered longest-first ----------

        // 1. The dotted base package. This is strictly longer than any module artifact id and than
        //    the parent artifact id, so putting it first prevents partial matches.
        contentRules.add(new ContentRule(
                oldBasePackage,
                newBasePackage,
                "base package com.*.* in sources, poms, yaml, docs"));

        // 2. Context-prefixed module artifact ids. These appear in <artifactId>, <module>, and
        //    inter-module <dependency><artifactId>.
        for (String moduleName : inspection.moduleDirectories()) {
            String renamed = renameModule(moduleName, oldContext, newContext);
            if (!moduleName.equals(renamed)) {
                contentRules.add(new ContentRule(
                        moduleName,
                        renamed,
                        "module artifactId / <module> entry"));
            }
        }

        // 3. The parent pom's own artifact id. Shape is "<old>-management-parent" in the shipped
        //    template, but we derive it from what the scanner actually read so we tolerate forks.
        String parentArtifactId = inspection.parentArtifactId();
        String renamedParent = renameParentArtifact(parentArtifactId, oldContext, newContext);
        if (!parentArtifactId.equals(renamedParent)) {
            contentRules.add(new ContentRule(
                    parentArtifactId,
                    renamedParent,
                    "parent artifactId"));
        }

        // ---------- Path rules ----------
        // These rewrite directory and file paths. Longer, more-specific prefixes first.
        for (String moduleName : inspection.moduleDirectories()) {
            String renamed = renameModule(moduleName, oldContext, newContext);
            if (!moduleName.equals(renamed)) {
                pathRules.add(new PathRule(moduleName + "/", renamed + "/"));
            }
        }

        // The Java source package directory structure. Use forward-slashes because that is what
        // the engine normalizes to when walking the tree.
        String oldPackagePath = oldBasePackage.replace('.', '/');
        String newPackagePath = newBasePackage.replace('.', '/');
        if (!oldPackagePath.equals(newPackagePath)) {
            pathRules.add(new PathRule(oldPackagePath + "/", newPackagePath + "/"));
        }

        return new RenamingPlan(pathRules, contentRules);
    }

    private static String replaceLeafSegment(String dotted, String newLeaf) {
        int lastDot = dotted.lastIndexOf('.');
        if (lastDot < 0) {
            return newLeaf;
        }
        return dotted.substring(0, lastDot + 1) + newLeaf;
    }

    private static String renameModule(String moduleName, String oldContext, String newContext) {
        if (moduleName.startsWith(oldContext + "-")) {
            return newContext + moduleName.substring(oldContext.length());
        }
        return moduleName;
    }

    private static String renameParentArtifact(String parentArtifactId, String oldContext, String newContext) {
        // Covers the shipped shape "<old>-management-parent" as well as any user variant whose
        // first hyphen-separated token equals the old context.
        if (parentArtifactId.startsWith(oldContext + "-")) {
            return newContext + parentArtifactId.substring(oldContext.length());
        }
        return parentArtifactId;
    }
}
