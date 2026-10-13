package com.enterprise.generator.web.rest;

import com.enterprise.generator.core.apply.GenerationReport;
import com.enterprise.generator.core.apply.TemplateCopier;
import com.enterprise.generator.core.plan.RenamingPlan;
import com.enterprise.generator.core.scan.TemplateScanner;
import com.enterprise.generator.core.spec.GenerationRequest;
import com.enterprise.generator.web.dto.GenerationRequestDto;
import com.enterprise.generator.web.dto.GenerationResponseDto;
import com.enterprise.generator.web.dto.PreviewResponseDto;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.validation.Valid;

/**
 * HTTP front door for the generator.
 *
 * <ul>
 *   <li>{@code POST /api/v1/preview} - validates and scans; returns the detected template shape
 *       and the computed renaming plan as JSON. Never writes anything.</li>
 *   <li>{@code POST /api/v1/generate} - validates, scans, plans, and (unless {@code dryRun} is
 *       set) writes a transformed copy. Returns the generation report as JSON.</li>
 * </ul>
 *
 * <p>Error translation is delegated to {@link ExceptionHandlers}, which turns the engine's
 * exceptions into RFC 7807 problem+json responses.
 */
@Controller("/api/v1")
public class GeneratorController {

    private final TemplateScanner scanner;
    private final TemplateCopier copier;

    public GeneratorController(TemplateScanner scanner, TemplateCopier copier) {
        this.scanner = scanner;
        this.copier = copier;
    }

    @Post("/preview")
    public HttpResponse<PreviewResponseDto> preview(@Body @Valid GenerationRequestDto dto) {
        GenerationRequest request = toRequest(dto);
        RenamingPlan plan = copier.planFor(request);
        return HttpResponse.ok(PreviewResponseDto.of(scanner.scan(request.source()), plan));
    }

    @Post("/generate")
    public HttpResponse<GenerationResponseDto> generate(@Body @Valid GenerationRequestDto dto) {
        GenerationRequest request = toRequest(dto);
        GenerationReport report = copier.generate(request);
        return HttpResponse.ok(GenerationResponseDto.of(report));
    }

    private static GenerationRequest toRequest(GenerationRequestDto dto) {
        return GenerationRequest.builder()
                .source(dto.source())
                .target(dto.target())
                .newContext(dto.newContext())
                .oldContext(dto.effectiveOldContext())
                .force(dto.effectiveForce())
                .dryRun(dto.effectiveDryRun())
                .build();
    }
}
