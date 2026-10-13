package com.enterprise.generator.core.spec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextNameTest {

    @Test
    void acceptsSimpleLowercaseName() {
        assertThat(ContextName.of("billing").asString()).isEqualTo("billing");
    }

    @Test
    void trimsWhitespace() {
        assertThat(ContextName.of("  billing  ").asString()).isEqualTo("billing");
    }

    @Test
    void acceptsDigitsAndUnderscoresAfterLeadingLetter() {
        assertThat(ContextName.of("billing_v2").asString()).isEqualTo("billing_v2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void rejectsBlank(String blank) {
        assertThatThrownBy(() -> ContextName.of(blank))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> ContextName.of(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Billing", "BILLING", "bi lling", "billing-v2", "1billing", "_billing", "billing.v2"})
    void rejectsPatternViolations(String bad) {
        assertThatThrownBy(() -> ContextName.of(bad))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must match");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "x"})
    void rejectsTooShort(String oneChar) {
        assertThatThrownBy(() -> ContextName.of(oneChar))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("length");
    }

    @Test
    void rejectsTooLong() {
        String tooLong = "a".repeat(ContextName.MAX_LENGTH + 1);
        assertThatThrownBy(() -> ContextName.of(tooLong))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("length");
    }

    @ParameterizedTest
    @ValueSource(strings = {"class", "public", "void", "null", "true", "record", "sealed", "string", "object"})
    void rejectsJavaReservedWords(String reserved) {
        assertThatThrownBy(() -> ContextName.of(reserved))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reserved word");
    }

    @Test
    void equalsAndHashCodeByValue() {
        assertThat(ContextName.of("billing")).isEqualTo(ContextName.of("billing"));
        assertThat(ContextName.of("billing").hashCode()).isEqualTo(ContextName.of("billing").hashCode());
        assertThat(ContextName.of("billing")).isNotEqualTo(ContextName.of("invoice"));
    }
}
