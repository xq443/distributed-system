package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PostingRequestThread implements Runnable {
  private static final int MAX_RETRIES = 5;

  @Override
  public void run() {
    for (int i = 0; i < MultiThreadWorker.REQUESTS_PER_THREAD; i++) {
      try {
        LiftRideEvent event = MultiThreadWorker.getEventQueue().take(); // Blocking call
        sendPostRequest(event);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void sendPostRequest(LiftRideEvent event) {
    int attempts = 0;
    boolean success = false;
    while (attempts < MAX_RETRIES && !success) {
      int responseCode = sendRequest(event);
      if (responseCode == 201) {
        MultiThreadWorker.getSuccessfulRequests().incrementAndGet();
        success = true;
      } else {
        MultiThreadWorker.getFailedRequests().incrementAndGet();
        attempts++;
      }
    }
  }

  private int sendRequest(LiftRideEvent event) {
    Gson gson = new Gson();
    String jsonInputString = gson.toJson(event); // Convert the LiftRideEvent object to JSON

    try {
      URL url = new URL("http://localhost:8080/SkierServlet_war_exploded/skiers"); // Replace with your actual URL
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setDoOutput(true);


      try (var os = conn.getOutputStream()) {
        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      return conn.getResponseCode();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return 500; // Simulate a server error for testing
    }
  }
}