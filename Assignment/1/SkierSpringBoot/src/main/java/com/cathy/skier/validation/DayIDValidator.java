package com.cathy.skier.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DayIDValidator implements ConstraintValidator<DayID, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null || value.isEmpty()) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate("DayID cannot be blank")
          .addConstraintViolation();
      return false;
    }
    return value.equals("1");
  }
}
