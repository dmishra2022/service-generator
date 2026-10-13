package com.enterprise.generator.core.scan;

import com.enterprise.generator.core.testsupport.FakeTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateScannerTest {

    @TempDir
    Path tmp;

    private final TemplateScanner scanner = new TemplateScanner();

    @Test
    void parsesParentArtifactIdAndBasePackageAndModules() throws Exception {
        Path root = FakeTemplate.writeDefault(tmp);

        TemplateInspection inspection = scanner.scan(root);

        assertThat(inspection.parentArtifactId()).isEqualTo("order-management-parent");
        assertThat(inspection.basePackage()).isEqualTo("com.enterprise.starter.order");
        assertThat(inspection.detectedOldContext()).isEqualTo("order");
        assertThat(inspection.moduleDirectories()).containsExactlyInAnyOrder(
                "order-domain", "order-application");
        assertThat(inspection.additionalModuleDirectories()).containsExactly("architecture-tests");
    }

    @Test
    void failsWhenPomMissing() throws Exception {
        Path empty = Files.createDirectory(tmp.resolve("empty"));
        assertThatThrownBy(() -> scanner.scan(empty))
                .isInstanceOf(TemplateScanException.class)
                .hasMessageContaining("pom.xml");
    }

    @Test
    void failsWhenBasePackagePropertyMissing() throws Exception {
        Path root = Files.createDirectory(tmp.resolve("no-prop"));
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                    <artifactId>order-management-parent</artifactId>
                    <modules><module>order-domain</module></modules>
                </project>
                """);
        assertThatThrownBy(() -> scanner.scan(root))
                .isInstanceOf(TemplateScanException.class)
                .hasMessageContaining("base.package");
    }

    @Test
    void failsWhenModuleDeclaredButDirectoryMissing() throws Exception {
        Path root = Files.createDirectory(tmp.resolve("broken"));
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                    <artifactId>order-management-parent</artifactId>
                    <properties>
                        <base.package>com.enterprise.starter.order</base.package>
                    </properties>
                    <modules><module>order-domain</module></modules>
                </project>
                """);
        Files.writeString(root.resolve("architecture.yml"), "schemaVersion: 1\n");
        assertThatThrownBy(() -> scanner.scan(root))
                .isInstanceOf(TemplateScanException.class)
                .hasMessageContaining("not present on disk");
    }

    @Test
    void recordsArchitectureYmlLocation() throws Exception {
        Path root = FakeTemplate.writeDefault(tmp);

        TemplateInspection inspection = scanner.scan(root);

        assertThat(inspection.architectureYml()).isEqualTo(root.resolve("architecture.yml"));
        assertThat(Files.isRegularFile(inspection.architectureYml())).isTrue();
    }

    @Test
    void failsWhenArchitectureYmlMissing() throws Exception {
        Path root = FakeTemplate.writeDefault(tmp);
        Files.delete(root.resolve("architecture.yml"));

        assertThatThrownBy(() -> scanner.scan(root))
                .isInstanceOf(TemplateScanException.class)
                .hasMessageContaining("architecture.yml")
                .hasMessageContaining("refuses to produce a service without an ArchGuard policy");
    }
}
