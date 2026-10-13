package com.enterprise.generator.core.scan;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of what the {@link TemplateScanner} found in a candidate template directory.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code root} - the directory that was scanned.</li>
 *   <li>{@code parentArtifactId} - the {@code <artifactId>} read from the root {@code pom.xml}.</li>
 *   <li>{@code basePackage} - the value of the parent pom's {@code <base.package>} property.</li>
 *   <li>{@code detectedOldContext} - the leaf package segment of {@code basePackage}; the string
 *       that will be replaced throughout.</li>
 *   <li>{@code moduleDirectories} - every directory under {@code root} that begins with
 *       {@code detectedOldContext + "-"} and contains a {@code pom.xml}.</li>
 *   <li>{@code additionalModuleDirectories} - module directories declared by the parent pom's
 *       {@code <module>} entries that do not start with the old context prefix (for example
 *       {@code architecture-tests}); these are preserved verbatim.</li>
 *   <li>{@code architectureYml} - the ArchGuard policy file at the root of the template. Required
 *       so every generated service inherits the architecture policy from day one.</li>
 * </ul>
 */
public record TemplateInspection(
        Path root,
        String parentArtifactId,
        String basePackage,
        String detectedOldContext,
        List<String> moduleDirectories,
        List<String> additionalModuleDirectories,
        Path architectureYml) {

    public TemplateInspection {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(parentArtifactId, "parentArtifactId must not be null");
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        Objects.requireNonNull(detectedOldContext, "detectedOldContext must not be null");
        moduleDirectories = List.copyOf(Objects.requireNonNull(moduleDirectories, "moduleDirectories must not be null"));
        additionalModuleDirectories = List.copyOf(Objects.requireNonNull(
                additionalModuleDirectories, "additionalModuleDirectories must not be null"));
        Objects.requireNonNull(architectureYml, "architectureYml must not be null");
    }
}
