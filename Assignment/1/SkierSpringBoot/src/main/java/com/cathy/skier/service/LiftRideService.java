package com.cathy.skier.service;

import com.cathy.skier.pojo.LiftRide;
import com.cathy.skier.validation.LiftID;
import java.util.List;

public interface LiftRideService {
  LiftRide getLiftRide(Long id);
  LiftRide saveCourse(LiftRide liftRide);
  void deleteLiftRide(Long id);
  List<LiftRide> getLiftRide();
}
