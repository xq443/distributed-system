package com.cathy.skier.exception;

public class LiftRideNotFoundException extends RuntimeException {
  public LiftRideNotFoundException(Long id) {
    super("The lift ride id '" + id + "' does not exist in our records");
  }
}
