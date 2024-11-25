package client1;

import io.swagger.client.model.LiftRide;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventGenerator implements Runnable{
    private static final int TOTAL_EVENTS = 200000;
    private final BlockingQueue<LiftRideEvent> eventQueue;

    public EventGenerator(BlockingQueue<LiftRideEvent> eventQueue) {
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        for (int i = 0; i < TOTAL_EVENTS; i++) {
            LiftRideEvent event = generateRandomLiftRideEvent();
            eventQueue.add(event);
        }
    }

    private LiftRideEvent generateRandomLiftRideEvent() {
        LiftRide liftRide = new LiftRide();
        liftRide.setLiftID((int) (Math.random() * 40) + 1); // Random liftID between 1 and 40
        liftRide.setTime((int) (Math.random() * 360) + 1);   // Random time between 1 and 360
        int skierID = (int) (Math.random() * 100000) + 1;    // Random skierID between 1 and 100000
        int resortID = (int) (Math.random() * 10) + 1;       // Random resortID between 1 and 10
        String seasonID = "2024";
        String dayID = "1";
        return new LiftRideEvent(liftRide, skierID, resortID, seasonID, dayID);
    }
}
