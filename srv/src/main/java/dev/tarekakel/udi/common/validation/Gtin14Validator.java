package dev.tarekakel.udi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Gtin14Validator implements ConstraintValidator<Gtin14, String> {

    private static final int LENGTH = 14;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || isValidGtin14(value);
    }

    /** GS1 check digit: weights 3,1,3,1... from the right-most payload digit; check = (10 - sum mod 10) mod 10. */
    public static boolean isValidGtin14(String value) {
        if (value.length() != LENGTH || !value.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int payloadLength = LENGTH - 1;
        int sum = 0;
        for (int i = 0; i < payloadLength; i++) {
            int weight = (payloadLength - i) % 2 == 1 ? 3 : 1;
            sum += Character.digit(value.charAt(i), 10) * weight;
        }
        int expected = (10 - sum % 10) % 10;
        return expected == Character.digit(value.charAt(LENGTH - 1), 10);
    }
}
