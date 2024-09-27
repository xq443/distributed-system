package com.cathy.part2;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.servlet.http.HttpServletResponse;

public class PostingRequestThread implements Runnable {
  private static final int MAX_RETRIES = 5;
  private static final HttpClient httpClient = HttpClient.newHttpClient(); // Create a shared HttpClient instance
  private static final Gson gson = new Gson(); // Create a Gson instance for JSON conversion

  @Override
  public void run() {
    for (int i = 0; i < MultiThreadWorker.REQUESTS_PER_THREAD; i++) {
      LiftRideEvent event = null;
      while (event == null) {
        event = MultiThreadWorker.getEventQueue().poll(); // Non-blocking call
        if (event == null) {
          try {
            Thread.sleep(10); // Backoff if no event is available
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            return; // Exit if interrupted
          }
        }
      }
      sendPostRequest(event);
    }
  }

  private void sendPostRequest(LiftRideEvent event) {
    int attempts = 0;
    boolean success = false;
    while (attempts < MAX_RETRIES && !success) {
      int responseCode = sendRequest(event);
      if (responseCode == HttpServletResponse.SC_CREATED) {
        MultiThreadWorker.getSuccessfulRequests().incrementAndGet();
        success = true;
      } else {
        MultiThreadWorker.getFailedRequests().incrementAndGet();
        attempts++;
      }
    }

    if (!success) {
      System.out.println("Failed to send request after " + MAX_RETRIES + " attempts: " + event);
    }
  }

  private int sendRequest(LiftRideEvent event) {
    String jsonInputString = gson.toJson(event); // Convert the LiftRideEvent object to JSON

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("http://34.212.217.208:8080/SkierServlet_war/skiers"))
          .timeout(Duration.ofMinutes(2))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return response.statusCode();
    } catch (Exception e) {
      System.out.println("Error sending request: " + e.getMessage());
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // Simulate a server error for testing
    }
  }
}
