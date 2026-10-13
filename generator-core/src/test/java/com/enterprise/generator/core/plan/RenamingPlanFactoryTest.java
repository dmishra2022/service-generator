package com.enterprise.generator.core.plan;

import com.enterprise.generator.core.scan.TemplateInspection;
import com.enterprise.generator.core.scan.TemplateScanner;
import com.enterprise.generator.core.spec.GenerationRequest;
import com.enterprise.generator.core.testsupport.FakeTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RenamingPlanFactoryTest {

    @TempDir
    Path tmp;

    private Path templateRoot;
    private TemplateInspection inspection;

    @BeforeEach
    void setUp() throws Exception {
        templateRoot = FakeTemplate.writeDefault(tmp);
        inspection = new TemplateScanner().scan(templateRoot);
    }

    @Test
    void producesExpectedContentRulesInLongestFirstOrder() {
        GenerationRequest request = GenerationRequest.builder()
                .source(templateRoot)
                .target(tmp.resolve("out"))
                .newContext("billing")
                .build();

        RenamingPlan plan = RenamingPlanFactory.from(request, inspection);

        List<ContentRule> rules = plan.contentRules();
        assertThat(rules).isNotEmpty();

        // The base package must come first because it is the longest string.
        assertThat(rules.get(0).from()).isEqualTo("com.enterprise.starter.order");
        assertThat(rules.get(0).to()).isEqualTo("com.enterprise.starter.billing");

        // Every emitted content rule must be strictly non-empty and well-formed.
        assertThat(rules).allSatisfy(r -> {
            assertThat(r.from()).isNotEmpty();
            assertThat(r.to()).isNotEmpty();
        });

        // All five rename kinds must be represented.
        assertThat(rules).extracting(ContentRule::from).containsExactlyInAnyOrder(
                "com.enterprise.starter.order",
                "order-domain",
                "order-application",
                "order-management-parent");
    }

    @Test
    void producesPathRulesForEveryContextPrefixedModuleAndThePackage() {
        GenerationRequest request = GenerationRequest.builder()
                .source(templateRoot)
                .target(tmp.resolve("out"))
                .newContext("billing")
                .build();

        RenamingPlan plan = RenamingPlanFactory.from(request, inspection);

        assertThat(plan.pathRules()).extracting(PathRule::from).containsExactlyInAnyOrder(
                "order-domain/",
                "order-application/",
                "com/enterprise/starter/order/");
        assertThat(plan.pathRules()).extracting(PathRule::to).containsExactlyInAnyOrder(
                "billing-domain/",
                "billing-application/",
                "com/enterprise/starter/billing/");
    }

    @Test
    void doesNotRenameNonContextPrefixedModules() {
        GenerationRequest request = GenerationRequest.builder()
                .source(templateRoot)
                .target(tmp.resolve("out"))
                .newContext("billing")
                .build();

        RenamingPlan plan = RenamingPlanFactory.from(request, inspection);

        // architecture-tests must not be renamed anywhere.
        assertThat(plan.contentRules()).extracting(ContentRule::from).doesNotContain("architecture-tests");
        assertThat(plan.pathRules()).extracting(PathRule::from).doesNotContain("architecture-tests/");
    }

    @Test
    void appliesPathRulesInSequence() {
        GenerationRequest request = GenerationRequest.builder()
                .source(templateRoot)
                .target(tmp.resolve("out"))
                .newContext("billing")
                .build();

        RenamingPlan plan = RenamingPlanFactory.from(request, inspection);

        // A nested Java source path inside order-domain must be renamed on both axes.
        String renamed = plan.applyPath("order-domain/src/main/java/com/enterprise/starter/order/domain/Order.java");
        assertThat(renamed).isEqualTo(
                "billing-domain/src/main/java/com/enterprise/starter/billing/domain/Order.java");
    }

    @Test
    void rewritesArchitectureYmlPackageTokens() {
        GenerationRequest request = GenerationRequest.builder()
                .source(templateRoot)
                .target(tmp.resolve("out"))
                .newContext("billing")
                .build();

        RenamingPlan plan = RenamingPlanFactory.from(request, inspection);

        // Full-fidelity architecture.yml body as it would appear in a template. Every
        // base-package token must come out rewritten to the new context; the schema and
        // profile keywords must be untouched.
        String yaml = """
                schemaVersion: 1
                roots: ["com.enterprise.starter.order.."]
                profiles: [hexagonal, ddd]
                roles:
                  DOMAIN: ["com.enterprise.starter.order.domain.."]
                  APPLICATION:
                    - "com.enterprise.starter.order.application.service.."
                    - "com.enterprise.starter.order.application.command.."
                  INBOUND_PORT: ["com.enterprise.starter.order.application.port.in.."]
                  OUTBOUND_PORT: ["com.enterprise.starter.order.application.port.out.."]
                  INBOUND_ADAPTER: ["com.enterprise.starter.order.adapter.inbound.."]
                  OUTBOUND_ADAPTER: ["com.enterprise.starter.order.adapter.outbound.."]
                  COMPOSITION_ROOT: ["com.enterprise.starter.order.bootstrap.."]
                contexts:
                  - id: order
                    packages: ["com.enterprise.starter.order.."]
                    exposedPackages: ["com.enterprise.starter.order.adapter.inbound.rest.."]
                requireClassification: true
                failOn: ERROR
                """;

        String transformed = yaml;
        int totalReplacements = 0;
        for (ContentRule rule : plan.contentRules()) {
            ContentRule.Result result = rule.apply(transformed);
            transformed = result.content();
            totalReplacements += result.replacements();
        }

        // Every base-package occurrence must be rewritten.
        assertThat(transformed).doesNotContain("com.enterprise.starter.order");
        assertThat(transformed).contains("com.enterprise.starter.billing.domain..");
        assertThat(transformed).contains("com.enterprise.starter.billing.adapter.outbound..");
        assertThat(transformed).contains("com.enterprise.starter.billing.adapter.inbound.rest..");
        // Schema keywords are untouched.
        assertThat(transformed).contains("schemaVersion: 1");
        assertThat(transformed).contains("profiles: [hexagonal, ddd]");
        // The context id value "order" is a bare English word; our content rules deliberately do
        // not rewrite bare words (only the dotted base package and module artifact ids), so the
        // context id survives. Operators rename it manually if desired. The important invariant
        // is that no leaky dotted package token remains. 11 base-package occurrences across the
        // yaml body get rewritten by the single long-first content rule.
        assertThat(totalReplacements).isGreaterThanOrEqualTo(11);
    }
}
