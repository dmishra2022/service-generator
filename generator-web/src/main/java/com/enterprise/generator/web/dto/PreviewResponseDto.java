package com.enterprise.generator.web.dto;

import com.enterprise.generator.core.plan.ContentRule;
import com.enterprise.generator.core.plan.PathRule;
import com.enterprise.generator.core.plan.RenamingPlan;
import com.enterprise.generator.core.scan.TemplateInspection;
import io.micronaut.core.annotation.Introspected;

import java.util.List;

/**
 * JSON view of a renaming plan plus the detected template shape. Returned by
 * {@code POST /api/v1/preview}.
 */
@Introspected
public record PreviewResponseDto(
        String detectedOldContext,
        String detectedBasePackage,
        String detectedParentArtifactId,
        List<String> contextModules,
        List<String> additionalModules,
        List<PathRuleView> pathRules,
        List<ContentRuleView> contentRules) {

    public static PreviewResponseDto of(TemplateInspection inspection, RenamingPlan plan) {
        List<PathRuleView> paths = plan.pathRules().stream().map(PathRuleView::of).toList();
        List<ContentRuleView> contents = plan.contentRules().stream().map(ContentRuleView::of).toList();
        return new PreviewResponseDto(
                inspection.detectedOldContext(),
                inspection.basePackage(),
                inspection.parentArtifactId(),
                inspection.moduleDirectories(),
                inspection.additionalModuleDirectories(),
                paths,
                contents);
    }

    @Introspected
    public record PathRuleView(String from, String to) {
        static PathRuleView of(PathRule r) {
            return new PathRuleView(r.from(), r.to());
        }
    }

    @Introspected
    public record ContentRuleView(String from, String to, String description) {
        static ContentRuleView of(ContentRule r) {
            return new ContentRuleView(r.from(), r.to(), r.description());
        }
    }
}
