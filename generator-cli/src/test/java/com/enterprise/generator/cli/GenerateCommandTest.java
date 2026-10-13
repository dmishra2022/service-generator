package com.enterprise.generator.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateCommandTest {

    @TempDir
    Path tmp;

    private Path source;
    private Path target;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void setUp() throws IOException {
        source = writeMinimalTemplate(tmp.resolve("ddd"));
        target = tmp.resolve("out");
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
    }

    @Test
    void generatesSuccessfullyAndReturnsZero() throws IOException {
        int code = run(
                "--source", source.toString(),
                "--target", target.toString(),
                "--context", "billing");

        String stdout = out.toString(StandardCharsets.UTF_8);
        String stderr = err.toString(StandardCharsets.UTF_8);
        assertThat(code).withFailMessage("stderr=%s", stderr).isEqualTo(0);
        assertThat(stdout).contains("Generation", "text_transformed");
        assertThat(target.resolve("billing-domain/pom.xml")).exists();
        assertThat(Files.readString(target.resolve("pom.xml")))
                .contains("<artifactId>billing-management-parent</artifactId>")
                .contains("<base.package>com.enterprise.starter.billing</base.package>");
    }

    @Test
    void validationFailureReturnsExitCodeTwo() {
        int code = run(
                "--source", source.toString(),
                "--target", target.toString(),
                "--context", "Billing");

        String stderr = err.toString(StandardCharsets.UTF_8);
        assertThat(code).isEqualTo(2);
        assertThat(stderr).contains("Validation failed").contains("newContext");
    }

    @Test
    void dryRunDoesNotWriteFiles() {
        int code = run(
                "--source", source.toString(),
                "--target", target.toString(),
                "--context", "billing",
                "--dry-run");

        assertThat(code).isEqualTo(0);
        assertThat(target).doesNotExist();
        String stdout = out.toString(StandardCharsets.UTF_8);
        assertThat(stdout).contains("Dry run").contains("Path rules").contains("Content rules");
    }

    @Test
    void planOnlyPrintsRulesAndStops() {
        int code = run(
                "--source", source.toString(),
                "--target", target.toString(),
                "--context", "billing",
                "--plan");

        String stdout = out.toString(StandardCharsets.UTF_8);
        assertThat(code).isEqualTo(0);
        assertThat(target).doesNotExist();
        assertThat(stdout)
                .contains("order-domain/")
                .contains("billing-domain/")
                .contains("com.enterprise.starter.order")
                .contains("com.enterprise.starter.billing");
    }

    @Test
    void invalidTemplateReturnsThree() throws IOException {
        Path brokenSource = Files.createDirectory(tmp.resolve("broken"));
        Files.writeString(brokenSource.resolve("pom.xml"), "<project><artifactId>x</artifactId></project>");

        int code = run(
                "--source", brokenSource.toString(),
                "--target", tmp.resolve("elsewhere").toString(),
                "--context", "billing");

        assertThat(code).isEqualTo(3);
        assertThat(err.toString(StandardCharsets.UTF_8)).contains("Template looks invalid");
    }

    @Test
    void verboseListsEveryFile() {
        int code = run(
                "--source", source.toString(),
                "--target", target.toString(),
                "--context", "billing",
                "--verbose");

        assertThat(code).isEqualTo(0);
        String stdout = out.toString(StandardCharsets.UTF_8);
        assertThat(stdout).contains("Files:").contains("billing-domain");
    }

    @Test
    void missingRequiredOptionIsReportedByPicocli() {
        int code = run("--source", source.toString(), "--context", "billing");
        assertThat(code).isNotZero();
    }

    private int run(String... args) {
        return GenerateCommand.exec(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    // A trimmed replica of the DDD starter sufficient for the CLI path. We keep this inline
    // rather than reaching into the generator-core test sources to avoid a cross-module test
    // dependency. architecture.yml is now mandatory because the generator refuses to produce a
    // service without an ArchGuard policy; the base-package tokens get rewritten by the content
    // rules at generation time.
    private static Path writeMinimalTemplate(Path root) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                    <artifactId>order-management-parent</artifactId>
                    <properties><base.package>com.enterprise.starter.order</base.package></properties>
                    <modules><module>order-domain</module></modules>
                </project>
                """);
        Files.writeString(root.resolve("architecture.yml"), """
                schemaVersion: 1
                roots: ["com.enterprise.starter.order.."]
                profiles: [hexagonal]
                roles:
                  DOMAIN: ["com.enterprise.starter.order.domain.."]
                requireClassification: true
                """);
        Path mod = Files.createDirectories(root.resolve("order-domain"));
        Files.writeString(mod.resolve("pom.xml"), """
                <project>
                    <parent><artifactId>order-management-parent</artifactId></parent>
                    <artifactId>order-domain</artifactId>
                </project>
                """);
        Path pkg = Files.createDirectories(mod.resolve("src/main/java/com/enterprise/starter/order/domain"));
        Files.writeString(pkg.resolve("Order.java"), """
                package com.enterprise.starter.order.domain;
                public final class Order { }
                """);
        return root;
    }
}
