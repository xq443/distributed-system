package com.cathy;

/**
 * response data
 * For responses with status codes except for 201, return messages in json format.
 */
public class ResponseData {

  // message
  private String message;

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ResponseData() {
  }

  public ResponseData(String message) {
    this.message = message;
  }
}