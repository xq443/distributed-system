package com.cathy;

public class Validation {
  // Helper methods for parameter validation
  boolean areParametersMissing(RequestData requestData) {
    if (requestData == null) {
      return true;
    }

    return requestData.getSkierID() == null ||
        requestData.getResortID() == null ||
        requestData.getLiftID() == null ||
        requestData.getSeasonID() == null ||
        requestData.getDayID() == null ||
        requestData.getTime() == null;
  }

  boolean areParametersValid(RequestData requestData) {
    return isValidSkierID(requestData.getSkierID()) &&
        isValidResortID(requestData.getResortID()) &&
        isValidLiftID(requestData.getLiftID()) &&
        isValidSeasonID(requestData.getSeasonID()) &&
        isValidDayID(requestData.getDayID()) &&
        isValidTimeID(requestData.getTime());
  }


  boolean isValidResortID(Integer resortID) {
    try {
      return resortID >= 1 && resortID <= 10;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  boolean isValidSeasonID(String seasonID) {
    return "2024".equals(seasonID);
  }

  boolean isValidDayID(String dayID) {
    try {
      int id = Integer.parseInt(dayID);
      return id == 1;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  boolean isValidSkierID(Integer skierID) {
    try {
      return skierID >= 1 && skierID <= 100000;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isValidLiftID(Integer liftID) {
    try {
      return liftID >= 1 && liftID <= 40;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isValidTimeID(Integer timeID) {
    try {
      return timeID >= 1 && timeID <= 360;
    } catch (NumberFormatException e) {
      return false;
    }
  }

}
