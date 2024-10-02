package com.cathy.skier.web;


import com.cathy.skier.pojo.LiftRide;
import com.cathy.skier.service.LiftRideService;
import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/skiers")
public class LiftRideController {
  LiftRideService liftRideService;

  @GetMapping("/{id}")
  public ResponseEntity<LiftRide> getLiftRide(@PathVariable Long id) {
    return new ResponseEntity<>(liftRideService.getLiftRide(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<LiftRide> saveLiftRide(@Valid @RequestBody LiftRide liftRide) {
    return new ResponseEntity<>(liftRideService.saveLiftRide(liftRide), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<HttpStatus> deleteLiftRide(@PathVariable Long id) {
    liftRideService.deleteLiftRide(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/all")
  public ResponseEntity<List<LiftRide>> getLiftRide() {
    return new ResponseEntity<>(liftRideService.getLiftRide(), HttpStatus.OK);
  }
}
