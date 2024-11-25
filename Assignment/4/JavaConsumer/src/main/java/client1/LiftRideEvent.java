package client1;
import io.swagger.client.model.LiftRide;

public class LiftRideEvent{
    private final LiftRide liftRide;
    private final int skierID;
    private final int resortID;
    private final String seasonID;
    private final String dayID;

    public LiftRideEvent(LiftRide liftRide, int skierID, int resortID, String seasonID, String dayID) {
        this.liftRide = liftRide;
        this.skierID = skierID;
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
    }

    public LiftRide getLiftRide() {
        return liftRide;
    }

    public int getSkierID() {
        return skierID;
    }

    public int getResortID() {
        return resortID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public String getDayID() {
        return dayID;
    }
}
