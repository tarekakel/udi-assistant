package dev.tarekakel.udi.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A UDI-DI in GS1 form: 14 digits whose last digit is a valid GS1 mod-10 check digit. Null is left to {@code @NotNull}. */
@Documented
@Constraint(validatedBy = Gtin14Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Gtin14 {

    String message() default "must be a 14-digit GTIN with a valid GS1 check digit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
