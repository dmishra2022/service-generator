package com.enterprise.generator.core.apply;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExclusionPolicyTest {

    @TempDir
    Path tmp;

    @Test
    void alwaysExcludesVcsAndIdeDirsAtAnyDepth() throws IOException {
        Path git = Files.createDirectories(tmp.resolve("repo/.git"));
        Path idea = Files.createDirectories(tmp.resolve("repo/.idea"));
        assertThat(ExclusionPolicy.excludeDirectory(git)).isTrue();
        assertThat(ExclusionPolicy.excludeDirectory(idea)).isTrue();
    }

    @Test
    void excludesBuildDirsOnlyAtModuleLevel() throws IOException {
        // A module-level target/ (sits next to pom.xml) must be excluded.
        Path module = Files.createDirectories(tmp.resolve("mod"));
        Files.writeString(module.resolve("pom.xml"), "<project/>");
        Path moduleTarget = Files.createDirectories(module.resolve("target"));
        assertThat(ExclusionPolicy.excludeDirectory(moduleTarget)).isTrue();
    }

    @Test
    void doesNotExcludeHexagonalPortOutDirectory() throws IOException {
        // The canonical "port/out" package inside an application layer must NOT be excluded.
        Path portOut = Files.createDirectories(
                tmp.resolve("mod/src/main/java/com/example/app/port/out"));
        Files.writeString(tmp.resolve("mod/pom.xml"), "<project/>");

        assertThat(ExclusionPolicy.excludeDirectory(portOut)).isFalse();
    }

    @Test
    void doesNotExcludeBuildDirDeeperThanModuleLevel() throws IOException {
        // A directory accidentally named "build" or "out" deep inside source code is not a build
        // output; keep it.
        Path deep = Files.createDirectories(tmp.resolve("mod/src/main/java/com/example/out"));
        Files.writeString(tmp.resolve("mod/pom.xml"), "<project/>");

        assertThat(ExclusionPolicy.excludeDirectory(deep)).isFalse();
    }

    @Test
    void skipsDsStoreFile() throws IOException {
        Path ds = Files.writeString(tmp.resolve(".DS_Store"), "junk");
        assertThat(ExclusionPolicy.excludeFile(ds)).isTrue();
    }
}
