package com.enterprise.generator.core.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Inspects a candidate template directory and produces a {@link TemplateInspection} describing
 * what will need renaming.
 *
 * <p>The scanner is intentionally regex-based rather than loading a full XML parser. The parent
 * pom shape it depends on is narrow (a plain {@code <artifactId>}, a {@code <properties>} block
 * with {@code <base.package>}, and a {@code <modules>} block) and the regexes have been chosen to
 * work with any reasonable formatting.
 */
public final class TemplateScanner {

    // Matches the first <artifactId> that appears BEFORE any <dependencies> or <dependencyManagement>
    // block. This is almost always the project's own artifactId in a well-formed pom.
    // We locate it by taking the first <artifactId> whose preceding sibling is not inside a
    // <dependency> or <plugin> element; the simplest way is to take the artifactId that follows the
    // first <project ...> opening tag and comes before <dependencies>, <dependencyManagement>,
    // <build>, <profiles>, or </project>.
    private static final Pattern PARENT_ARTIFACT_ID =
            Pattern.compile("<project[^>]*>(?:.*?)<artifactId>([^<]+)</artifactId>", Pattern.DOTALL);

    private static final Pattern BASE_PACKAGE_PROPERTY =
            Pattern.compile("<base\\.package>([^<]+)</base\\.package>");

    private static final Pattern MODULE_ENTRY =
            Pattern.compile("<module>([^<]+)</module>");

    /**
     * Scan the given directory. Throws {@link TemplateScanException} with a precise message if the
     * directory does not look like the expected template.
     */
    public TemplateInspection scan(Path root) {
        if (root == null) {
            throw new TemplateScanException("root must not be null");
        }
        if (!Files.isDirectory(root)) {
            throw new TemplateScanException("not a directory: " + root);
        }

        Path pom = root.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            throw new TemplateScanException("missing pom.xml: " + pom);
        }

        String pomContent;
        try {
            pomContent = Files.readString(pom);
        } catch (IOException e) {
            throw new TemplateScanException("cannot read pom.xml: " + pom, e);
        }

        String parentArtifactId = requireFirstGroup(
                PARENT_ARTIFACT_ID, pomContent, "parent pom is missing <artifactId>");
        String basePackage = requireFirstGroup(
                BASE_PACKAGE_PROPERTY,
                pomContent,
                "parent pom is missing the <base.package> property; this is required so the "
                        + "generator can detect the current bounded context");

        String detected = leafSegment(basePackage);
        if (detected.isEmpty()) {
            throw new TemplateScanException(
                    "cannot derive the current context from base.package='" + basePackage + "'");
        }

        List<String> declaredModules = findAll(MODULE_ENTRY, pomContent);
        if (declaredModules.isEmpty()) {
            throw new TemplateScanException(
                    "parent pom declares no <module> entries; nothing to rename");
        }

        // ArchGuard policy carries over to every generated service. Fail closed if the template
        // omits it: the generator refuses to produce a service that has no architecture policy
        // because that would silently strip the enforcement from new bounded contexts.
        Path architectureYml = root.resolve("architecture.yml");
        if (!Files.isRegularFile(architectureYml)) {
            throw new TemplateScanException(
                    "missing architecture.yml at the template root: " + architectureYml
                            + ". The generator refuses to produce a service without an ArchGuard "
                            + "policy; add architecture.yml to the template or create one from "
                            + "examples/micronaut/architecture.yml in the ArchGuard SDK");
        }

        List<String> contextModules = new ArrayList<>();
        List<String> additionalModules = new ArrayList<>();
        String prefix = detected + "-";
        for (String module : declaredModules) {
            if (module.startsWith(prefix)) {
                contextModules.add(module);
            } else {
                additionalModules.add(module);
            }
        }

        // Confirm each context-prefixed module actually exists on disk.
        List<String> missing = new ArrayList<>();
        for (String module : contextModules) {
            if (!Files.isDirectory(root.resolve(module))) {
                missing.add(module);
            }
        }
        if (!missing.isEmpty()) {
            throw new TemplateScanException(
                    "parent pom references modules that are not present on disk: " + missing);
        }

        // Also pick up any directory on disk that matches the prefix but is not declared in the
        // parent (defensive: a user may have added a module and forgotten to list it; we rename it
        // anyway to keep the tree consistent).
        try (Stream<Path> entries = Files.list(root)) {
            entries
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(prefix))
                    .filter(name -> !contextModules.contains(name))
                    .forEach(contextModules::add);
        } catch (IOException e) {
            throw new TemplateScanException("cannot list " + root, e);
        }

        return new TemplateInspection(
                root,
                parentArtifactId,
                basePackage,
                detected,
                contextModules,
                additionalModules,
                architectureYml);
    }

    private static String requireFirstGroup(Pattern pattern, String content, String errorMessage) {
        Matcher m = pattern.matcher(content);
        if (!m.find()) {
            throw new TemplateScanException(errorMessage);
        }
        return m.group(1).trim();
    }

    private static List<String> findAll(Pattern pattern, String content) {
        List<String> out = new ArrayList<>();
        Matcher m = pattern.matcher(content);
        while (m.find()) {
            out.add(m.group(1).trim());
        }
        return out;
    }

    private static String leafSegment(String dotted) {
        int lastDot = dotted.lastIndexOf('.');
        return lastDot < 0 ? dotted : dotted.substring(lastDot + 1);
    }
}
