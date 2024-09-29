package com.cathy.skier.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SeasonIDValidator implements ConstraintValidator<SeasonID, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null || value.isEmpty()) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate("SeasonID cannot be blank")
          .addConstraintViolation();
      return false;
    }
    return value.equals("2024");
  }
}
