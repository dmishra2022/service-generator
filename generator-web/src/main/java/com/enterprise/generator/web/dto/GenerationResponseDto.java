package com.enterprise.generator.web.dto;

import com.enterprise.generator.core.apply.FileChange;
import com.enterprise.generator.core.apply.GenerationReport;
import io.micronaut.core.annotation.Introspected;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JSON view of a {@link GenerationReport}. Keeps the full file list so a user can audit which
 * files were touched; the UI uses this for the "What changed" panel.
 */
@Introspected
public record GenerationResponseDto(
        String source,
        String target,
        boolean dryRun,
        int total,
        Map<String, Integer> counts,
        String summary,
        List<FileChangeView> changes) {

    public static GenerationResponseDto of(GenerationReport report) {
        Map<String, Integer> counts = new TreeMap<>();
        for (FileChange.Kind k : FileChange.Kind.values()) {
            counts.put(k.name().toLowerCase(), report.count(k));
        }
        List<FileChangeView> changes = report.changes().stream().map(FileChangeView::of).toList();
        return new GenerationResponseDto(
                report.source().toString(),
                report.target().toString(),
                report.dryRun(),
                report.total(),
                counts,
                report.summary(),
                changes);
    }

    @Introspected
    public record FileChangeView(String kind, String source, String target, int replacements) {
        static FileChangeView of(FileChange c) {
            return new FileChangeView(
                    c.kind().name().toLowerCase(),
                    c.sourceRelative(),
                    c.targetRelative(),
                    c.replacements());
        }
    }
}
