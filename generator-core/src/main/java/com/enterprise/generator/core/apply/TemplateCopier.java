package com.enterprise.generator.core.apply;

import com.enterprise.generator.core.plan.ContentRule;
import com.enterprise.generator.core.plan.RenamingPlan;
import com.enterprise.generator.core.plan.RenamingPlanFactory;
import com.enterprise.generator.core.scan.TemplateInspection;
import com.enterprise.generator.core.scan.TemplateScanner;
import com.enterprise.generator.core.spec.GenerationRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The orchestrator. Takes a validated {@link GenerationRequest}, scans the template, builds a
 * {@link RenamingPlan}, walks the source tree, and writes a transformed copy to the target
 * directory.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>No destructive operation runs during the validation or scan phase.</li>
 *   <li>In dry-run mode, no file-system write occurs anywhere.</li>
 *   <li>In write mode, if the target directory did not exist before the run and the copy fails
 *       part way through, the partially-written tree is removed. If the target already existed
 *       (and {@code force=true} was passed), no rollback is attempted since the user explicitly
 *       opted in to overwriting an existing directory.</li>
 * </ul>
 */
public final class TemplateCopier {

    private static final Logger log = LoggerFactory.getLogger(TemplateCopier.class);

    private final TemplateScanner scanner;

    public TemplateCopier() {
        this(new TemplateScanner());
    }

    public TemplateCopier(TemplateScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
    }

    /** Build a plan for a request without writing anything; useful for the /preview endpoint. */
    public RenamingPlan planFor(GenerationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        TemplateInspection inspection = scanner.scan(request.source());
        return RenamingPlanFactory.from(request, inspection);
    }

    /**
     * Execute the request. Honours {@link GenerationRequest#dryRun()}.
     *
     * @throws GenerationException on any I/O failure during the copy.
     */
    public GenerationReport generate(GenerationRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        TemplateInspection inspection = scanner.scan(request.source());
        RenamingPlan plan = RenamingPlanFactory.from(request, inspection);
        GenerationReport report = new GenerationReport(request.source(), request.target(), request.dryRun());

        boolean targetExistedBefore = Files.exists(request.target());

        if (!request.dryRun()) {
            try {
                Files.createDirectories(request.target());
            } catch (IOException e) {
                throw new GenerationException("cannot create target directory: " + request.target(), e);
            }
        }

        try {
            walkAndCopy(request, plan, report);
        } catch (RuntimeException e) {
            if (!request.dryRun() && !targetExistedBefore) {
                safeDeleteRecursively(request.target());
            }
            throw e;
        }

        return report.freeze();
    }

    private void walkAndCopy(GenerationRequest request, RenamingPlan plan, GenerationReport report) {
        Path source = request.source();
        Path target = request.target();

        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(source)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (ExclusionPolicy.excludeDirectory(dir)) {
                        String rel = source.relativize(dir).toString();
                        report.record(new FileChange(
                                FileChange.Kind.SKIPPED,
                                normalize(rel),
                                normalize(rel),
                                0));
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String rel = source.relativize(dir).toString();
                    String renamed = plan.applyPath(normalize(rel));
                    Path destination = target.resolve(renamed);
                    if (!request.dryRun()) {
                        Files.createDirectories(destination);
                    }
                    report.record(new FileChange(
                            FileChange.Kind.DIRECTORY_CREATED,
                            normalize(rel),
                            renamed,
                            0));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (ExclusionPolicy.excludeFile(file)) {
                        String rel = source.relativize(file).toString();
                        report.record(new FileChange(
                                FileChange.Kind.SKIPPED,
                                normalize(rel),
                                normalize(rel),
                                0));
                        return FileVisitResult.CONTINUE;
                    }
                    String rel = source.relativize(file).toString();
                    String renamed = plan.applyPath(normalize(rel));
                    Path destination = target.resolve(renamed);

                    try {
                        copyOrTransform(file, destination, rel, renamed, plan, request.dryRun(), report);
                    } catch (IOException e) {
                        throw new GenerationException(
                                "failed to copy " + file + " -> " + destination + ": " + e.getMessage(),
                                e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    failure.compareAndSet(null, new GenerationException(
                            "cannot visit " + file + ": " + exc.getMessage(), exc));
                    return FileVisitResult.TERMINATE;
                }
            });
        } catch (IOException e) {
            throw new GenerationException("walking the template failed: " + e.getMessage(), e);
        }

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    private static void copyOrTransform(
            Path source,
            Path destination,
            String sourceRelative,
            String targetRelative,
            RenamingPlan plan,
            boolean dryRun,
            GenerationReport report) throws IOException {

        boolean isText = FileClassifier.isText(source);
        if (!isText) {
            if (!dryRun) {
                Files.createDirectories(destination.getParent());
                Files.copy(source, destination);
            }
            report.record(new FileChange(
                    FileChange.Kind.BINARY_COPIED,
                    normalize(sourceRelative),
                    targetRelative,
                    0));
            return;
        }

        String original = Files.readString(source, StandardCharsets.UTF_8);
        int totalReplacements = 0;
        String transformed = original;
        for (ContentRule rule : plan.contentRules()) {
            ContentRule.Result result = rule.apply(transformed);
            transformed = result.content();
            totalReplacements += result.replacements();
        }

        if (!dryRun) {
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, transformed, StandardCharsets.UTF_8);
        }

        FileChange.Kind kind = totalReplacements > 0
                ? FileChange.Kind.TEXT_TRANSFORMED
                : FileChange.Kind.TEXT_UNCHANGED;
        report.record(new FileChange(kind, normalize(sourceRelative), targetRelative, totalReplacements));
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    private static void safeDeleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        Deque<Path> stack = new ArrayDeque<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    stack.push(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    stack.push(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            while (!stack.isEmpty()) {
                Path p = stack.pop();
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("rollback: cannot delete {}: {}", p, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("rollback walk failed on {}: {}", root, e.getMessage());
        }
    }
}
