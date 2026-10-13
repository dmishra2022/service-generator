package com.enterprise.generator.core.apply;

import java.nio.file.Path;
import java.util.Set;

/**
 * Decides whether a path encountered during template traversal should be skipped entirely. The
 * default policy excludes common generated or VCS-local directories so a user who runs the
 * generator against a working checkout does not get {@code target/} and {@code .git/} copied into
 * the new project.
 *
 * <p>Two distinct lists:
 * <ul>
 *   <li>{@code ALWAYS_EXCLUDED} - names that are never valid as source directories at any depth
 *       in a Java project (for example {@code .git}, {@code .idea}, {@code node_modules}).</li>
 *   <li>{@code BUILD_DIR_NAMES} - names that look like build outputs ({@code target},
 *       {@code build}, {@code out}, {@code .gradle}) and are only excluded when they are a
 *       <em>module-level</em> directory, i.e. a sibling of a {@code pom.xml} or of
 *       {@code src/}. This avoids catching legitimate source packages such as the hexagonal
 *       {@code port/out} convention.</li>
 * </ul>
 */
public final class ExclusionPolicy {

    private static final Set<String> ALWAYS_EXCLUDED = Set.of(
            ".git", ".idea", ".vscode", ".settings", ".mvn",
            "node_modules",
            ".DS_Store");

    private static final Set<String> BUILD_DIR_NAMES = Set.of(
            "target", "build", "out", ".gradle");

    private static final Set<String> EXCLUDED_FILE_NAMES = Set.of(
            ".DS_Store", "Thumbs.db");

    private ExclusionPolicy() {
    }

    /** @return {@code true} if the directory should be pruned from traversal. */
    public static boolean excludeDirectory(Path dir) {
        if (dir.getFileName() == null) {
            return false;
        }
        String name = dir.getFileName().toString();
        if (ALWAYS_EXCLUDED.contains(name)) {
            return true;
        }
        if (BUILD_DIR_NAMES.contains(name) && isModuleLevelDirectory(dir)) {
            return true;
        }
        return false;
    }

    /** @return {@code true} if the file should be skipped. */
    public static boolean excludeFile(Path file) {
        String name = file.getFileName() == null ? "" : file.getFileName().toString();
        return EXCLUDED_FILE_NAMES.contains(name);
    }

    /**
     * A directory is "module-level" if it sits next to a {@code pom.xml} or a {@code src}
     * directory. That is where Maven and Gradle place their build output and where the exclusion
     * is safe. Anywhere deeper (for example {@code .../application/port/out}) is a real source
     * directory and must be kept.
     */
    private static boolean isModuleLevelDirectory(Path dir) {
        Path parent = dir.getParent();
        if (parent == null) {
            return false;
        }
        return java.nio.file.Files.isRegularFile(parent.resolve("pom.xml"))
                || java.nio.file.Files.isDirectory(parent.resolve("src"));
    }
}
