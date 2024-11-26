package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;

import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PostingRequestThread implements Runnable {
  private static final int MAX_RETRIES = 5;
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient(); // Shared HttpClient instance
  private static final Gson GSON = new Gson(); // Gson instance for JSON conversion
  private static final String POST_URL = "http://44.245.24.217:8080/SkierServlet_war/skiers"; // Endpoint URL

  private final BlockingQueue<LiftRideEvent> queue;
  private final AtomicInteger successfulRequests;
  private final AtomicInteger failedRequests;
  private final CountDownLatch latch;

  // Constructor to receive necessary parameters
  public PostingRequestThread(BlockingQueue<LiftRideEvent> queue,
      AtomicInteger successfulRequests,
      AtomicInteger failedRequests,
      CountDownLatch latch) {
    this.queue = queue;
    this.successfulRequests = successfulRequests;
    this.failedRequests = failedRequests;
    this.latch = latch;
  }

  @Override
  public void run() {
    try {
      // Each thread will handle requests until the queue is empty
      while (true) {
        LiftRideEvent liftRide = queue.poll(); // Retrieve an event from the queue
        if (liftRide == null) {
          break; // Exit if no more events are available
        }
        // Try sending the POST request
        sendPostRequest(liftRide);
        latch.countDown(); // Count down for each request sent
      }
    } catch (Exception e) {
      System.err.println("Error in PostingRequestThread: " + e.getMessage());
    }
  }

  private void sendPostRequest(LiftRideEvent event) {
    int attempts = 0;
    boolean success = false;

    while (attempts < MAX_RETRIES && !success) {
      try {
        int responseCode = sendRequest(event);

        if (responseCode == HttpServletResponse.SC_CREATED) {
          successfulRequests.incrementAndGet();
          success = true;
        } else {
          failedRequests.incrementAndGet();
          attempts++;
        }
      } catch (Exception e) {
        System.err.println("Exception when calling API for " + event + ": " + e.getMessage());
      }
    }

    // Log failure if max retries are reached
    if (!success) {
      System.err.println("Failed to send request after " + MAX_RETRIES + " attempts: " + event);
    }
  }

  private int sendRequest(LiftRideEvent event) {
    String jsonInputString = GSON.toJson(event); // Convert LiftRideEvent to JSON

    try {
      // Build the POST request
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(POST_URL))
          .timeout(Duration.ofSeconds(10)) // Adjusted to 10 seconds for timeout
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
          .build();

      // Send the request and handle the response
      HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      return response.statusCode();
    } catch (Exception e) {
      System.err.println("Error sending request: " + e.getMessage());
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // Return error status
    }
  }
}
