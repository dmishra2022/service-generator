package com.enterprise.generator.core.scan;

/**
 * Thrown by {@link TemplateScanner} when the directory does not look like a valid Micronaut DDD
 * starter: missing pom.xml, missing parent artifactId, missing {@code <base.package>} property,
 * malformed XML, or no modules found.
 */
public class TemplateScanException extends RuntimeException {

    public TemplateScanException(String message) {
        super(message);
    }

    public TemplateScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
