package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import java.util.Random;

public class EventGenerator implements Runnable {
  private static final Random random = new Random();

  @Override
  public void run() {
    // Generate lift ride events and add to the queue
    for (int i = 0; i < MultiThreadWorker.TOTAL_REQUESTS; i++) {
      LiftRideEvent event = new LiftRideEvent(
          random.nextInt(10) + 1,          // resortID: between 1 and 10
          "2024",                          // seasonID: fixed to 2024
          "1",                             // dayID: fixed to 1
          random.nextInt(360) + 1,         // time: between 1 and 360
          random.nextInt(100000) + 1,      // skierID: between 1 and 100000
          random.nextInt(40) + 1           // liftID: between 1 and 40
      );
      try {
        MultiThreadWorker.getEventQueue().put(event);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
