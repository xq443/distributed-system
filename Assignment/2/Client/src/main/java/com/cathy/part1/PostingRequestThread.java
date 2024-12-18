package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;

import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadPoolExecutor;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CountDownLatch;

public class PostingRequestThread implements Runnable {
  private static final int MAX_RETRIES = 5;
  private static final HttpClient httpClient = HttpClient.newHttpClient(); // Shared HttpClient instance
  private static final Gson gson = new Gson(); // Gson instance for JSON conversion
  private static final String POST_URL = "http://alb-586000881.us-west-2.elb.amazonaws.com/SkierServlet_war/skiers"; // Endpoint URL
  private final CountDownLatch latch;
  private final ThreadPoolExecutor executor;

  // Constructor to receive latch
  public PostingRequestThread(CountDownLatch latch, ThreadPoolExecutor executor) {
    this.latch = latch;
    this.executor = executor;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < MultiThreadWorker.REQUESTS_PER_THREAD; i++) {
        LiftRideEvent liftRide = MultiThreadWorker.getEventQueue().poll();
        if (liftRide == null) {
          break; // Break if no more events are available
        }
        // Try sending the POST request
        sendPostRequest(liftRide);
        latch.countDown(); // Count down for each request sent
      }

      // If this thread is the first to complete
      if (!MultiThreadWorker.isPhaseOneComplete()) {
        MultiThreadWorker.markPhaseOneComplete();

        // Adjust thread pool to handle the remaining requests
        for (int i = 0; i < MultiThreadWorker.PHASE_TWO_THREADS - MultiThreadWorker.INITIAL_THREADS; i++) {
          executor.execute(new PostingRequestThread(latch, executor)); // Start the second phase threading
        }
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
        System.err.println("Exception when calling API for " + event + ": " + e.getMessage());
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
