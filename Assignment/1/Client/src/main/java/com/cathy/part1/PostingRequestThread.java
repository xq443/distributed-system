package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;

import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CountDownLatch;

public class PostingRequestThread implements Runnable {
  private static final int MAX_RETRIES = 5;
  private static final HttpClient httpClient = HttpClient.newHttpClient(); // Shared HttpClient instance
  private static final Gson gson = new Gson(); // Gson instance for JSON conversion
  private static final String POST_URL = "http://localhost:8080/SkierServlet_war_exploded/skiers"; // Endpoint URL
  private final CountDownLatch latch;

  // Constructor to receive latch
  public PostingRequestThread(CountDownLatch latch) {
    this.latch = latch;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < MultiThreadWorker.REQUESTS_PER_THREAD_INITIAL; i++) {
        LiftRideEvent liftRide = MultiThreadWorker.getEventQueue().poll();
        if (liftRide == null) {
          break;
        }
        // Try sending the POST request
        sendPostRequest(liftRide);
      }

      // If this thread is the first to complete
      if (!MultiThreadWorker.isFirstThreadCompleted()) {
        MultiThreadWorker.setFirstThreadCompleted(true);
        latch.countDown(); // Signal that a thread has completed
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void sendPostRequest(LiftRideEvent event) {
    int attempts = 0;
    boolean success = false;

    while (attempts < MAX_RETRIES && !success) {
      try {
        int responseCode = sendRequest(event);

        if (responseCode == HttpServletResponse.SC_CREATED) {
          MultiThreadWorker.getSuccessfulRequests().incrementAndGet();
          success = true;
        } else {
          MultiThreadWorker.getFailedRequests().incrementAndGet();
          attempts++;
        }
      } catch (Exception e) {
        System.err.println("Exception when calling API for " + event + e.getMessage());
      }
    }

    // Log failure if max retries are reached
    if (!success) {
      System.err.println("Failed to send request after " + MAX_RETRIES + " attempts: " + event);
    }
  }

  private int sendRequest(LiftRideEvent event) {
    String jsonInputString = gson.toJson(event); // Convert LiftRideEvent to JSON

    try {
      // Build the POST request
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(POST_URL))
          .timeout(Duration.ofSeconds(10)) // Adjusted to 10 seconds for timeout
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
          .build();

      // Send the request and handle the response
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return response.statusCode();
    } catch (Exception e) {
      System.err.println("Error sending request: " + e.getMessage());
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // Return error status
    }
  }
}
