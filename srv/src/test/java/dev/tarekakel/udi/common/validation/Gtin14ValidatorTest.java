package dev.tarekakel.udi.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Gtin14ValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"04012345678901", "04098765432101", "04000000000006"})
    void acceptsValidCheckDigits(String gtin) {
        assertThat(Gtin14Validator.isValidGtin14(gtin)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"04012345678902", "0401234567890", "040123456789012", "0401234567890A"})
    void rejectsWrongCheckDigitLengthOrCharacters(String gtin) {
        assertThat(Gtin14Validator.isValidGtin14(gtin)).isFalse();
    }
}
