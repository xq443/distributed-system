package com.cathy;

public class SkierLiftRide {
  private int skierId;
  private int resortId;
  private int liftId;
  private int time;
  private int dayId;

  public SkierLiftRide(int skierId, int resortId, int liftId, int time, int dayId) {
    this.skierId = skierId;
    this.resortId = resortId;
    this.liftId = liftId;
    this.time = time;
    this.dayId = dayId;
  }

  // Getters and setters
  public int getSkierId() { return skierId; }
  public void setSkierId(int skierId) { this.skierId = skierId; }

  public int getResortId() { return resortId; }
  public void setResortId(int resortId) { this.resortId = resortId; }

  public int getLiftId() { return liftId; }
  public void setLiftId(int liftId) { this.liftId = liftId; }

  public int getTime() { return time; }
  public void setTime(int time) { this.time = time; }

  public int getDayId() { return dayId; }
  public void setDayId(int dayId) { this.dayId = dayId; }
}