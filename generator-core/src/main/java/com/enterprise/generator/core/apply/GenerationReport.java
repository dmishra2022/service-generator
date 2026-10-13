package com.enterprise.generator.core.apply;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The outcome of a generation run. Reports every file that was created, copied, transformed, or
 * skipped, and keeps counters for a quick textual summary.
 *
 * <p>Reports are mutable while a run is in progress and frozen on completion via {@link #freeze()}.
 */
public final class GenerationReport {

    private final Path source;
    private final Path target;
    private final boolean dryRun;
    private final List<FileChange> changes = new ArrayList<>();
    private final EnumMap<FileChange.Kind, Integer> counts = new EnumMap<>(FileChange.Kind.class);
    private boolean frozen = false;

    public GenerationReport(Path source, Path target, boolean dryRun) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.dryRun = dryRun;
    }

    void record(FileChange change) {
        if (frozen) {
            throw new IllegalStateException("report is frozen");
        }
        Objects.requireNonNull(change, "change must not be null");
        changes.add(change);
        counts.merge(change.kind(), 1, Integer::sum);
    }

    GenerationReport freeze() {
        this.frozen = true;
        return this;
    }

    public Path source() {
        return source;
    }

    public Path target() {
        return target;
    }

    public boolean dryRun() {
        return dryRun;
    }

    public List<FileChange> changes() {
        return Collections.unmodifiableList(changes);
    }

    public int count(FileChange.Kind kind) {
        return counts.getOrDefault(kind, 0);
    }

    /** @return the total number of files/directories recorded (any kind). */
    public int total() {
        return changes.size();
    }

    /** @return a human-readable summary suitable for CLI output or an HTTP response field. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(dryRun ? "Dry run " : "Generation ")
                .append("from ").append(source).append('\n')
                .append("                 to ").append(target).append('\n');
        for (Map.Entry<FileChange.Kind, Integer> e : counts.entrySet()) {
            sb.append(String.format("  %-22s %d%n", e.getKey().name().toLowerCase(), e.getValue()));
        }
        sb.append(String.format("  %-22s %d%n", "total", total()));
        return sb.toString();
    }
}
