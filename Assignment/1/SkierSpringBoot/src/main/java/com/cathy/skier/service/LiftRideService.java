package com.cathy.skier.service;

import com.cathy.skier.pojo.LiftRide;
import java.util.List;

public interface LiftRideService {
  LiftRide getLiftRide(Long id);
  LiftRide saveLiftRide(LiftRide liftRide);
  void deleteLiftRide(Long id);
  List<LiftRide> getLiftRide();
}
