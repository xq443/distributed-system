package com.cathy.skier.validation;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ResortIDValidator implements ConstraintValidator<ResortID, Integer> {

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate("ResortID cannot be blank")
          .addConstraintViolation();
      return false;
    }
    return value >= 1 && value <= 10;
  }
}
