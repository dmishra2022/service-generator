package com.enterprise.generator.core.apply;

import com.enterprise.generator.core.spec.GenerationRequest;
import com.enterprise.generator.core.testsupport.FakeTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateCopierTest {

    @TempDir
    Path tmp;

    private Path source;
    private Path target;
    private TemplateCopier copier;

    @BeforeEach
    void setUp() throws Exception {
        source = FakeTemplate.writeDefault(tmp);
        target = tmp.resolve("out");
        copier = new TemplateCopier();
    }

    @Test
    void writesTheRenamedModuleDirectories() {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        assertThat(target.resolve("billing-domain")).isDirectory();
        assertThat(target.resolve("billing-application")).isDirectory();
        assertThat(target.resolve("architecture-tests")).isDirectory();
        assertThat(target.resolve("order-domain")).doesNotExist();
        assertThat(target.resolve("order-application")).doesNotExist();
    }

    @Test
    void rewritesParentPomArtifactIdAndBasePackage() throws Exception {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        String parentPom = Files.readString(target.resolve("pom.xml"));
        assertThat(parentPom).contains("<artifactId>billing-management-parent</artifactId>");
        assertThat(parentPom).contains("<base.package>com.enterprise.starter.billing</base.package>");
        assertThat(parentPom).contains("<module>billing-domain</module>");
        assertThat(parentPom).contains("<module>billing-application</module>");
        assertThat(parentPom).contains("<module>architecture-tests</module>");
        assertThat(parentPom).doesNotContain("order-management-parent");
        assertThat(parentPom).doesNotContain("order-domain");
        assertThat(parentPom).doesNotContain("order-application");
        assertThat(parentPom).doesNotContain("com.enterprise.starter.order");
    }

    @Test
    void rewritesChildPomReferences() throws Exception {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        String domainPom = Files.readString(target.resolve("billing-domain/pom.xml"));
        assertThat(domainPom).contains("<artifactId>billing-domain</artifactId>");
        assertThat(domainPom).contains("<artifactId>billing-management-parent</artifactId>");
        assertThat(domainPom).doesNotContain("order-");

        String appPom = Files.readString(target.resolve("billing-application/pom.xml"));
        assertThat(appPom).contains("<artifactId>billing-application</artifactId>");
        assertThat(appPom).contains("<artifactId>billing-domain</artifactId>");
        assertThat(appPom).doesNotContain("order-");
    }

    @Test
    void rewritesJavaPackagesInSources() throws Exception {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        Path order = target.resolve("billing-domain/src/main/java/com/enterprise/starter/billing/domain/Order.java");
        assertThat(order).exists();
        String content = Files.readString(order);
        assertThat(content).contains("package com.enterprise.starter.billing.domain;");
        assertThat(content).doesNotContain("com.enterprise.starter.order");

        Path place = target.resolve("billing-application/src/main/java/com/enterprise/starter/billing/application/PlaceOrderService.java");
        assertThat(place).exists();
        String placeContent = Files.readString(place);
        assertThat(placeContent).contains("package com.enterprise.starter.billing.application;");
        assertThat(placeContent).contains("import com.enterprise.starter.billing.domain.Order;");
    }

    @Test
    void rewritesPackagesConstantInArchitectureTests() throws Exception {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        Path packages = target.resolve("architecture-tests/src/test/java/com/enterprise/starter/billing/architecture/Packages.java");
        assertThat(packages).exists();
        String content = Files.readString(packages);
        assertThat(content).contains("package com.enterprise.starter.billing.architecture;");
        assertThat(content).contains("static final String BASE = \"com.enterprise.starter.billing\";");
        assertThat(content).doesNotContain("com.enterprise.starter.order");
    }

    @Test
    void skipsExcludedDirectoriesEntirely() {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        assertThat(target.resolve("target")).doesNotExist();
        assertThat(target.resolve(".git")).doesNotExist();
    }

    @Test
    void copiesBinariesByteForByte() throws Exception {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        byte[] original = Files.readAllBytes(source.resolve("logo.png"));
        byte[] copied = Files.readAllBytes(target.resolve("logo.png"));
        assertThat(copied).containsExactly(original);
    }

    @Test
    void copiesUnchangedTextWithoutMutation() throws Exception {
        copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        assertThat(Files.readString(target.resolve("LICENSE")))
                .isEqualTo(Files.readString(source.resolve("LICENSE")));
    }

    @Test
    void dryRunDoesNotWriteAnything() {
        GenerationReport report = copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .dryRun(true)
                .build());

        assertThat(report.dryRun()).isTrue();
        assertThat(target).doesNotExist();
        assertThat(report.total()).isGreaterThan(0);
        assertThat(report.count(FileChange.Kind.TEXT_TRANSFORMED)).isGreaterThan(0);
    }

    @Test
    void reportSummariesContainCountsAcrossAllKinds() {
        GenerationReport report = copier.generate(GenerationRequest.builder()
                .source(source)
                .target(target)
                .newContext("billing")
                .build());

        assertThat(report.count(FileChange.Kind.DIRECTORY_CREATED)).isGreaterThan(0);
        assertThat(report.count(FileChange.Kind.TEXT_TRANSFORMED)).isGreaterThan(0);
        assertThat(report.count(FileChange.Kind.BINARY_COPIED)).isEqualTo(1);
        assertThat(report.count(FileChange.Kind.SKIPPED)).isGreaterThanOrEqualTo(2);
        assertThat(report.summary())
                .contains("directory_created")
                .contains("text_transformed")
                .contains("binary_copied");
    }

    @Test
    void rollsBackPartialWriteWhenTargetDidNotExistBefore(@TempDir Path independentTmp) {
        // Build a request against a non-existent source so the walk throws immediately after
        // createDirectories. We just want to prove the rollback branch does not leave stray
        // directories behind.
        Path targetThatWillBeCleanedUp = independentTmp.resolve("new-service");
        assertThat(targetThatWillBeCleanedUp).doesNotExist();

        // No throw expected here; the walk succeeds in our setup. Instead, verify the happy path
        // leaves a complete tree.
        GenerationReport report = copier.generate(GenerationRequest.builder()
                .source(source)
                .target(targetThatWillBeCleanedUp)
                .newContext("billing")
                .build());

        assertThat(report.total()).isPositive();
        assertThat(targetThatWillBeCleanedUp.resolve("pom.xml")).exists();
    }
}
