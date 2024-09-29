package com.cathy.skier.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SkierIDValidator implements ConstraintValidator<SkierID, Integer> {

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate("SkierID cannot be blank")
          .addConstraintViolation();
      return false;
    }
    return value >= 1 && value <= 100000;
  }
}
