package com.enterprise.generator.cli;

import com.enterprise.generator.core.apply.FileChange;
import com.enterprise.generator.core.apply.GenerationException;
import com.enterprise.generator.core.apply.GenerationReport;
import com.enterprise.generator.core.apply.TemplateCopier;
import com.enterprise.generator.core.plan.ContentRule;
import com.enterprise.generator.core.plan.PathRule;
import com.enterprise.generator.core.plan.RenamingPlan;
import com.enterprise.generator.core.scan.TemplateScanException;
import com.enterprise.generator.core.spec.GenerationRequest;
import com.enterprise.generator.core.spec.ValidationException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * The single CLI command: generate a renamed copy of the starter.
 *
 * <p>Usage shape:
 * <pre>
 *   service-generator \
 *     --source /path/to/ddd \
 *     --target /path/to/billing-service \
 *     --context billing \
 *     [--old-context order] [--force] [--dry-run] [--verbose]
 * </pre>
 *
 * <p>Exit codes:
 * <ul>
 *   <li>{@code 0} - success.</li>
 *   <li>{@code 2} - validation failure (bad input).</li>
 *   <li>{@code 3} - the source directory did not look like a valid template.</li>
 *   <li>{@code 4} - generation failed partway through (I/O or similar).</li>
 *   <li>{@code 1} - unexpected error.</li>
 * </ul>
 */
@Command(
        name = "service-generator",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Clone the Micronaut DDD + Hexagonal starter into a new bounded context, "
                + "renaming module directories, Maven artifact IDs, Java packages, and the "
                + "Packages.BASE constant in a single atomic operation.")
public final class GenerateCommand implements Callable<Integer> {

    @Option(names = {"-s", "--source"}, required = true,
            description = "Path to the template directory (must contain a parent pom.xml with "
                    + "<base.package>).")
    Path source;

    @Option(names = {"-t", "--target"}, required = true,
            description = "Path where the renamed project will be written. Must not be the same "
                    + "as --source or inside it. If the directory exists and is non-empty, pass "
                    + "--force.")
    Path target;

    @Option(names = {"-c", "--context"}, required = true,
            description = "The new bounded-context name (lowercase, Java-identifier safe, 2-32 "
                    + "chars).")
    String newContext;

    @Option(names = {"--old-context"},
            description = "The context name present in the template. Defaults to 'order'; "
                    + "override this if the template was itself generated.")
    String oldContext = "order";

    @Option(names = {"-f", "--force"},
            description = "Allow writing into a non-empty target directory.")
    boolean force;

    @Option(names = {"-n", "--dry-run"},
            description = "Print the plan and the summary without writing any file.")
    boolean dryRun;

    @Option(names = {"-v", "--verbose"},
            description = "List every file that was touched, with substitution counts.")
    boolean verbose;

    @Option(names = {"--plan"},
            description = "Print the computed renaming plan (path and content rules) and exit.")
    boolean planOnly;

    // Overridable for tests.
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    public void withStreams(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public Integer call() {
        try {
            GenerationRequest request = GenerationRequest.builder()
                    .source(source)
                    .target(target)
                    .newContext(newContext)
                    .oldContext(oldContext)
                    .force(force)
                    .dryRun(dryRun)
                    .build();

            TemplateCopier copier = new TemplateCopier();

            if (planOnly) {
                RenamingPlan plan = copier.planFor(request);
                printPlan(request, plan);
                return 0;
            }

            if (dryRun) {
                RenamingPlan plan = copier.planFor(request);
                printPlan(request, plan);
                out.println();
            }

            GenerationReport report = copier.generate(request);
            out.print(report.summary());
            if (verbose) {
                out.println();
                out.println("Files:");
                for (FileChange change : report.changes()) {
                    out.printf("  %-20s %4d  %s%s%n",
                            change.kind().name().toLowerCase(),
                            change.replacements(),
                            change.sourceRelative(),
                            change.sourceRelative().equals(change.targetRelative())
                                    ? ""
                                    : "  ->  " + change.targetRelative());
                }
            }
            return 0;
        } catch (ValidationException e) {
            err.println("Validation failed:");
            e.errors().forEach(fe -> err.println("  - " + fe));
            return 2;
        } catch (TemplateScanException e) {
            err.println("Template looks invalid: " + e.getMessage());
            return 3;
        } catch (GenerationException e) {
            err.println("Generation failed: " + e.getMessage());
            if (verbose && e.getCause() != null) {
                e.getCause().printStackTrace(err);
            }
            return 4;
        } catch (RuntimeException e) {
            err.println("Unexpected error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(err);
            }
            return 1;
        }
    }

    private void printPlan(GenerationRequest request, RenamingPlan plan) {
        out.println("Plan for " + request.source() + " -> " + request.target());
        out.println("  new context: " + request.newContext());
        out.println("  old context: " + request.oldContext());
        out.println();
        out.println("Path rules (" + plan.pathRules().size() + "):");
        for (PathRule rule : plan.pathRules()) {
            out.printf("  %s   ->   %s%n", rule.from(), rule.to());
        }
        out.println();
        out.println("Content rules (" + plan.contentRules().size() + "):");
        for (ContentRule rule : plan.contentRules()) {
            out.printf("  %-40s ->  %-40s  (%s)%n", rule.from(), rule.to(), rule.description());
        }
    }

    // Keep picocli-compatible: the CommandLine framework invokes call(); we expose this helper so
    // tests can bypass construction of the CommandLine entirely.
    static int exec(String[] args, PrintStream out, PrintStream err) {
        GenerateCommand cmd = new GenerateCommand();
        cmd.withStreams(out, err);
        CommandLine cl = new CommandLine(cmd);
        cl.setOut(new java.io.PrintWriter(out, true));
        cl.setErr(new java.io.PrintWriter(err, true));
        return cl.execute(args);
    }
}
