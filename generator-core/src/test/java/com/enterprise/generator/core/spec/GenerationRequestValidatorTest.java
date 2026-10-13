package com.enterprise.generator.core.spec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationRequestValidatorTest {

    @TempDir
    Path tmp;

    private Path validSource;
    private Path parentDir;

    @BeforeEach
    void setUp() throws Exception {
        validSource = Files.createDirectory(tmp.resolve("template"));
        Files.writeString(validSource.resolve("pom.xml"), "<project><artifactId>x</artifactId></project>");
        parentDir = Files.createDirectory(tmp.resolve("out"));
    }

    @Test
    void acceptsValidInput() {
        GenerationRequest request = GenerationRequest.builder()
                .source(validSource)
                .target(parentDir.resolve("billing"))
                .newContext("billing")
                .build();

        assertThat(request.newContext().asString()).isEqualTo("billing");
        assertThat(request.oldContext().asString()).isEqualTo("order");
        assertThat(request.source()).isEqualTo(validSource.toAbsolutePath().normalize());
        assertThat(request.force()).isFalse();
        assertThat(request.dryRun()).isFalse();
    }

    @Test
    void collectsAllErrorsAtOnce() {
        assertThatThrownBy(() -> GenerationRequest.builder()
                .source("")
                .target("")
                .newContext("Billing")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).extracting(ValidationException.FieldError::field)
                            .contains("source", "target", "newContext");
                });
    }

    @Test
    void rejectsMissingSource() {
        assertThatThrownBy(() -> GenerationRequest.builder()
                .source(tmp.resolve("does-not-exist"))
                .target(parentDir.resolve("billing"))
                .newContext("billing")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).anyMatch(e ->
                            e.field().equals("source") && e.message().contains("does not exist"));
                });
    }

    @Test
    void rejectsSourceWithoutPomXml() throws Exception {
        Path noPom = Files.createDirectory(tmp.resolve("not-maven"));
        assertThatThrownBy(() -> GenerationRequest.builder()
                .source(noPom)
                .target(parentDir.resolve("billing"))
                .newContext("billing")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).anyMatch(e ->
                            e.field().equals("source") && e.message().contains("pom.xml"));
                });
    }

    @Test
    void rejectsTargetEqualToSource() {
        assertThatThrownBy(() -> GenerationRequest.builder()
                .source(validSource)
                .target(validSource)
                .newContext("billing")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).anyMatch(e ->
                            e.field().equals("target") && e.message().contains("must differ"));
                });
    }

    @Test
    void rejectsTargetNestedInsideSource() {
        assertThatThrownBy(() -> GenerationRequest.builder()
                .source(validSource)
                .target(validSource.resolve("sub"))
                .newContext("billing")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).anyMatch(e ->
                            e.field().equals("target") && e.message().contains("nested"));
                });
    }

    @Test
    void rejectsNonEmptyTargetWithoutForce() throws Exception {
        Path existing = Files.createDirectory(parentDir.resolve("billing"));
        Files.writeString(existing.resolve("README.md"), "hello");

        assertThatThrownBy(() -> GenerationRequest.builder()
                .source(validSource)
                .target(existing)
                .newContext("billing")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).anyMatch(e ->
                            e.field().equals("target") && e.message().contains("not empty"));
                });
    }

    @Test
    void acceptsNonEmptyTargetWithForce() throws Exception {
        Path existing = Files.createDirectory(parentDir.resolve("billing"));
        Files.writeString(existing.resolve("README.md"), "hello");

        GenerationRequest request = GenerationRequest.builder()
                .source(validSource)
                .target(existing)
                .newContext("billing")
                .force(true)
                .build();

        assertThat(request.force()).isTrue();
    }

    @Test
    void acceptsEmptyExistingTarget() throws Exception {
        Path existing = Files.createDirectory(parentDir.resolve("billing"));

        GenerationRequest request = GenerationRequest.builder()
                .source(validSource)
                .target(existing)
                .newContext("billing")
                .build();

        assertThat(request.target()).isEqualTo(existing.toAbsolutePath().normalize());
    }

    @Test
    void rejectsMissingParentOfTarget() {
        assertThatThrownBy(() -> GenerationRequest.builder()
                .source(validSource)
                .target(tmp.resolve("does").resolve("not").resolve("exist"))
                .newContext("billing")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).anyMatch(e -> e.field().equals("target"));
                });
    }

    @Test
    void rejectsNewContextEqualToOldContext() {
        assertThatThrownBy(() -> GenerationRequest.builder()
                .source(validSource)
                .target(parentDir.resolve("order"))
                .newContext("order")
                .build())
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.errors()).anyMatch(e ->
                            e.field().equals("newContext") && e.message().contains("differ"));
                });
    }

    @Test
    void honoursExplicitOldContext() {
        GenerationRequest request = GenerationRequest.builder()
                .source(validSource)
                .target(parentDir.resolve("v2"))
                .newContext("billing_v2")
                .oldContext("billing")
                .build();

        assertThat(request.oldContext().asString()).isEqualTo("billing");
        assertThat(request.newContext().asString()).isEqualTo("billing_v2");
    }
}
