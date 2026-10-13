package com.enterprise.generator.core.spec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaReservedWordsTest {

    @Test
    void detectsKeywords() {
        assertThat(JavaReservedWords.isReserved("class")).isTrue();
        assertThat(JavaReservedWords.isReserved("public")).isTrue();
        assertThat(JavaReservedWords.isReserved("void")).isTrue();
    }

    @Test
    void detectsLiterals() {
        assertThat(JavaReservedWords.isReserved("null")).isTrue();
        assertThat(JavaReservedWords.isReserved("true")).isTrue();
    }

    @Test
    void detectsRestrictedIdentifiers() {
        assertThat(JavaReservedWords.isReserved("record")).isTrue();
        assertThat(JavaReservedWords.isReserved("sealed")).isTrue();
        assertThat(JavaReservedWords.isReserved("yield")).isTrue();
    }

    @Test
    void detectsJavaLangTypes() {
        assertThat(JavaReservedWords.isReserved("string")).isTrue();
        assertThat(JavaReservedWords.isReserved("object")).isTrue();
        assertThat(JavaReservedWords.isReserved("thread")).isTrue();
    }

    @Test
    void isCaseInsensitive() {
        assertThat(JavaReservedWords.isReserved("CLASS")).isTrue();
        assertThat(JavaReservedWords.isReserved("Public")).isTrue();
    }

    @Test
    void acceptsOrdinaryNames() {
        assertThat(JavaReservedWords.isReserved("billing")).isFalse();
        assertThat(JavaReservedWords.isReserved("order")).isFalse();
        assertThat(JavaReservedWords.isReserved("inventory")).isFalse();
        assertThat(JavaReservedWords.isReserved("customer")).isFalse();
    }

    @Test
    void handlesNullSafely() {
        assertThat(JavaReservedWords.isReserved(null)).isFalse();
    }
}
