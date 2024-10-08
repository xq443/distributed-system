package com.cathy.part2;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;

import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CountDownLatch;
import java.io.PrintWriter;

public class PostingRequestThread implements Runnable {
  private static final int MAX_RETRIES = 5;
  private static final HttpClient httpClient = HttpClient.newHttpClient(); // Shared HttpClient instance
  private static final Gson gson = new Gson(); // Gson instance for JSON conversion
  private static final String POST_URL = "http://54.190.44.136:8080/SkierServlet_war/skiers"; // Endpoint URL
  private final CountDownLatch latch;
  private final ConcurrentLinkedQueue<Long> latencies;
  private final PrintWriter fileWriter;

  // Constructor to receive latch and fileWriter
  public PostingRequestThread(CountDownLatch latch, ConcurrentLinkedQueue<Long> latencies, PrintWriter fileWriter) {
    this.latch = latch;
    this.latencies = latencies;
    this.fileWriter = fileWriter;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < MultiThreadWorker.REQUESTS_PER_THREAD; i++) {
        LiftRideEvent liftRide = MultiThreadWorker.getEventQueue().poll();

        if (liftRide == null) {
          break;
        }
        // Try sending the POST request
        sendPostRequest(liftRide);
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void sendPostRequest(LiftRideEvent event) {
    int attempts = 0;
    boolean success = false;

    while (attempts < MAX_RETRIES && !success) {
      long startTime = System.currentTimeMillis();
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
      } finally {
        long latency = System.currentTimeMillis() - startTime;
        latencies.add(latency);
        synchronized (fileWriter) {
          fileWriter.println(startTime + ",POST," + latency + "," + (success ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
        this.latch.countDown();
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
