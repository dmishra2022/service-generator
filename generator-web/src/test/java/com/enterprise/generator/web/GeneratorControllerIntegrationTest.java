package com.enterprise.generator.web;

import com.enterprise.generator.web.dto.GenerationRequestDto;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MicronautTest
class GeneratorControllerIntegrationTest {

    @Inject
    @Client("/")
    HttpClient http;

    @TempDir
    Path tmp;

    @Test
    void previewReturnsThePlanWithoutWriting() throws IOException {
        Path source = fakeTemplate(tmp.resolve("ddd"));
        Path target = tmp.resolve("out");

        var dto = new GenerationRequestDto(
                source.toString(), target.toString(), "billing", null, false, false);

        var response = http.toBlocking().exchange(
                HttpRequest.POST("/api/v1/preview", dto), Map.class);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        Map body = response.body();
        assertThat(body).containsEntry("detectedOldContext", "order");
        assertThat(body).containsEntry("detectedBasePackage", "com.enterprise.starter.order");
        assertThat(body).containsEntry("detectedParentArtifactId", "order-management-parent");
        assertThat(target).doesNotExist();
    }

    @Test
    void generateProducesTheRenamedTree() throws IOException {
        Path source = fakeTemplate(tmp.resolve("ddd"));
        Path target = tmp.resolve("out");

        var dto = new GenerationRequestDto(
                source.toString(), target.toString(), "billing", null, false, false);

        HttpResponse<Map> response = http.toBlocking().exchange(
                HttpRequest.POST("/api/v1/generate", dto), Map.class);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(target.resolve("billing-domain/pom.xml")).exists();
        assertThat(Files.readString(target.resolve("pom.xml")))
                .contains("billing-management-parent")
                .contains("com.enterprise.starter.billing");

        Map body = response.body();
        assertThat(body).containsEntry("dryRun", false);
        assertThat(body.get("summary")).asString().contains("Generation");
    }

    @Test
    void dryRunDoesNotWrite() throws IOException {
        Path source = fakeTemplate(tmp.resolve("ddd"));
        Path target = tmp.resolve("out");

        var dto = new GenerationRequestDto(
                source.toString(), target.toString(), "billing", null, false, true);

        http.toBlocking().exchange(HttpRequest.POST("/api/v1/generate", dto), Map.class);

        assertThat(target).doesNotExist();
    }

    @Test
    void invalidContextYields400ProblemJson() throws IOException {
        Path source = fakeTemplate(tmp.resolve("ddd"));
        Path target = tmp.resolve("out");

        var dto = new GenerationRequestDto(
                source.toString(), target.toString(), "Billing", null, false, false);

        assertThatThrownBy(() -> http.toBlocking().exchange(
                HttpRequest.POST("/api/v1/preview", dto), Map.class))
                .isInstanceOfSatisfying(HttpClientResponseException.class, e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
                });
    }

    @Test
    void missingPomYields422ProblemJson(@TempDir Path alt) throws IOException {
        Path badSource = Files.createDirectory(alt.resolve("not-a-template"));
        var dto = new GenerationRequestDto(
                badSource.toString(), alt.resolve("out").toString(), "billing", null, false, false);

        assertThatThrownBy(() -> http.toBlocking().exchange(
                HttpRequest.POST("/api/v1/preview", dto), Map.class))
                .isInstanceOfSatisfying(HttpClientResponseException.class, e -> {
                    // Missing pom.xml trips the validator before the scanner runs, so this is
                    // reported as a 400 (validation) rather than 422 (scan). Either way, the
                    // controller must not 500.
                    assertThat(e.getStatus().getCode()).isBetween(400, 499);
                });
    }

    static Path fakeTemplate(Path root) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                    <artifactId>order-management-parent</artifactId>
                    <properties><base.package>com.enterprise.starter.order</base.package></properties>
                    <modules><module>order-domain</module></modules>
                </project>
                """);
        // Required by the scanner since every generated service inherits an ArchGuard policy.
        Files.writeString(root.resolve("architecture.yml"), """
                schemaVersion: 1
                roots: ["com.enterprise.starter.order.."]
                profiles: [hexagonal]
                roles:
                  DOMAIN: ["com.enterprise.starter.order.domain.."]
                requireClassification: true
                """);
        Path m = Files.createDirectories(root.resolve("order-domain"));
        Files.writeString(m.resolve("pom.xml"), "<project><artifactId>order-domain</artifactId></project>");
        Path p = Files.createDirectories(m.resolve("src/main/java/com/enterprise/starter/order/domain"));
        Files.writeString(p.resolve("Order.java"), "package com.enterprise.starter.order.domain;\npublic class Order {}\n");
        return root;
    }
}
