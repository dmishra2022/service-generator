package com.enterprise.generator.core.spec;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The exhaustive set of identifiers that are illegal (or reserved) as a Java package segment or
 * class name. Used by {@link GenerationRequestValidator} to prevent generated code from failing to
 * compile.
 *
 * <p>This list is deliberately conservative: it includes keywords, boolean/null literals,
 * contextual keywords (restricted identifiers), and a handful of core {@code java.lang} class names
 * that would shadow built-ins if used as a bounded-context package segment.
 */
public final class JavaReservedWords {

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while");

    private static final Set<String> LITERALS = Set.of("true", "false", "null");

    private static final Set<String> RESTRICTED_IDENTIFIERS = Set.of(
            "var", "yield", "record", "sealed", "permits", "non-sealed", "exports",
            "module", "open", "opens", "provides", "requires", "to", "transitive",
            "uses", "with");

    private static final Set<String> JAVA_LANG_TYPES = Set.of(
            "object", "string", "integer", "long", "double", "float", "boolean", "byte",
            "short", "character", "number", "void", "class", "enum", "record", "thread",
            "runnable", "system", "math", "error", "exception", "runtimeexception",
            "throwable", "iterable", "comparable", "cloneable", "process", "runtime");

    private static final Set<String> ALL;

    static {
        Set<String> all = new HashSet<>();
        all.addAll(KEYWORDS);
        all.addAll(LITERALS);
        all.addAll(RESTRICTED_IDENTIFIERS);
        all.addAll(JAVA_LANG_TYPES);
        ALL = Collections.unmodifiableSet(all);
    }

    private JavaReservedWords() {
    }

    /**
     * @return {@code true} if the identifier (compared case-insensitively) collides with a Java
     *     keyword, literal, restricted identifier, or a common {@code java.lang} type name.
     */
    public static boolean isReserved(String identifier) {
        if (identifier == null) {
            return false;
        }
        return ALL.contains(identifier.toLowerCase());
    }

    /** @return an immutable view of every reserved term; mostly useful for diagnostics. */
    public static Set<String> all() {
        return ALL;
    }
}
