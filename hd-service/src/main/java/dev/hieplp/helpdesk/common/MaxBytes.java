package dev.hieplp.helpdesk.common;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/** String length in UTF-8 bytes, not chars (bcrypt truncates at 72 bytes). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxBytes.Validator.class)
public @interface MaxBytes {

  /**
   * Maximum allowed UTF-8 byte length.
   *
   * @return the limit
   */
  int value();

  /**
   * Validation failure message.
   *
   * @return the message
   */
  String message() default "exceeds byte limit";

  /**
   * Validation groups.
   *
   * @return the groups
   */
  Class<?>[] groups() default {};

  /**
   * Validation payload.
   *
   * @return the payload
   */
  Class<? extends Payload>[] payload() default {};

  /** Checks a string's UTF-8 byte length against the annotation's limit. */
  class Validator implements ConstraintValidator<MaxBytes, String> {

    private int max;

    /** Stores the configured byte limit. */
    @Override
    public void initialize(MaxBytes annotation) {
      max = annotation.value();
    }

    /**
     * @param value candidate string; null passes (use {@code @NotBlank} for presence)
     * @return true when the UTF-8 byte length is within the limit
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
  }
}
