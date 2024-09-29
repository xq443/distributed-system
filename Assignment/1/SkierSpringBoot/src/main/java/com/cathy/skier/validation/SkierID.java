package com.cathy.skier.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SkierIDValidator.class)

public @interface SkierID {
  String message() default "The SkierID is not valid";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
