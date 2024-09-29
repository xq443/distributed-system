package com.cathy.skier.validation;



import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class LiftIDValidator implements ConstraintValidator<LiftID, Integer> {

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate("LiftID cannot be blank")
          .addConstraintViolation();
      return false;
    }
    return value >= 1 && value <= 40;
  }
}
