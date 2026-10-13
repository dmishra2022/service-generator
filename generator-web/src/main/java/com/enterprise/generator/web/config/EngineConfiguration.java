package com.enterprise.generator.web.config;

import com.enterprise.generator.core.apply.TemplateCopier;
import com.enterprise.generator.core.scan.TemplateScanner;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/**
 * Wires the framework-free generator engine into the Micronaut context. We keep this in one tiny
 * factory to preserve the invariant that {@code generator-core} depends on nothing framework-y.
 */
@Factory
public class EngineConfiguration {

    @Singleton
    public TemplateScanner templateScanner() {
        return new TemplateScanner();
    }

    @Singleton
    public TemplateCopier templateCopier(TemplateScanner scanner) {
        return new TemplateCopier(scanner);
    }
}
