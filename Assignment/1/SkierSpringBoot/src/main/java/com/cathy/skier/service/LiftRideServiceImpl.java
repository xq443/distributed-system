package com.cathy.skier.service;

import com.cathy.skier.exception.LiftRideNotFoundException;
import com.cathy.skier.pojo.LiftRide;
import com.cathy.skier.repository.LiftRideRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class LiftRideServiceImpl implements LiftRideService {

  LiftRideRepository liftRideRepository;

  @Override
  public LiftRide getLiftRide(Long id) {
    Optional<LiftRide> liftRide = liftRideRepository.findById(id);
    if (liftRide.isPresent()) {
      return liftRide.get();
    } else {
      throw new LiftRideNotFoundException(id);
    }
  }

  @Override
  public LiftRide saveLiftRide(LiftRide liftRide) {
    return liftRideRepository.save(liftRide);
  }

  @Override
  public void deleteLiftRide(Long id) {
    liftRideRepository.deleteById(id);
  }

  @Override
  public List<LiftRide> getLiftRide() {
    return (List<LiftRide>)liftRideRepository.findAll();
  }
}
