package com.enterprise.generator.web;

import io.micronaut.runtime.Micronaut;

/**
 * Micronaut entry point for the service generator's HTTP front door.
 *
 * <p>By default binds {@code 0.0.0.0:8080} (see {@code application.yml}). The static UI is served
 * from {@code classpath:public/index.html}; the API is rooted at {@code /api/v1}.
 */
public final class Application {

    private Application() {
    }

    public static void main(String[] args) {
        Micronaut.build(args)
                .mainClass(Application.class)
                .start();
    }
}
