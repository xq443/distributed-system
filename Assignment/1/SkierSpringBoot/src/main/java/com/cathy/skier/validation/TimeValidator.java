package com.cathy.skier.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TimeValidator implements ConstraintValidator<Time, Integer> {

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null) {        // Customize the error message for null values
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate("Time cannot be blank")
          .addConstraintViolation();
      return false;
    }
    return value >= 1 && value <= 360;
  }
}
