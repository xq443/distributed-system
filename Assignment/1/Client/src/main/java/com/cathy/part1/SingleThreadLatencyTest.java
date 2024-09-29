package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.servlet.http.HttpServletResponse;

public class SingleThreadLatencyTest {
  private static final int TOTAL_REQUESTS = 10000;
  private static final HttpClient httpClient = HttpClient.newHttpClient();
  private static final Gson gson = new Gson();
  private static final String POST_URL = "http://localhost:8080/skiers"; // Endpoint URL "http://54.190.44.136:8080/SkierServlet_war/skiers"


  public static void main(String[] args) {
    long startTime = System.currentTimeMillis(); // Start time for total run

    int successfulRequests = 0;
    int failedRequests = 0;

    for (int i = 0; i < TOTAL_REQUESTS; i++) {
      LiftRideEvent event = generateLiftRideEvent(i); // Generate a LiftRideEvent
      int responseCode = sendRequest(event);

      if (responseCode == HttpServletResponse.SC_CREATED) {
        successfulRequests++;
      } else {
        failedRequests++;
        System.out.println("Request " + (i + 1) + " failed with response code: " + responseCode);
      }
    }

    long totalTime = System.currentTimeMillis() - startTime; // End time for total run

    System.out.println("Total time for " + TOTAL_REQUESTS + " requests: " + totalTime + " ms");
    System.out.printf("Average latency per request: %.2f ms%n", (totalTime / (double) TOTAL_REQUESTS));
    System.out.println("Number of successful requests: " + successfulRequests);
    System.out.println("Number of unsuccessful requests: " + failedRequests);
    System.out.printf("Total throughput: %.2f requests/second%n", (TOTAL_REQUESTS / (totalTime / 1000.0)));
  }

  private static LiftRideEvent generateLiftRideEvent(int index) {
    return new LiftRideEvent(
        (int) (Math.random() * 10) + 1, // resortID between 1 and 10
        "2024",
        "1",
        (int) (Math.random() * 360) + 1, // time between 1 and 360
        (int) (Math.random() * 100_000) + 1, // skierID between 1 and 100000
        (int) (Math.random() * 40) + 1 // liftID between 1 and 40
    );
  }

  private static int sendRequest(LiftRideEvent event) {
    String jsonInputString = gson.toJson(event); // Convert the LiftRideEvent object to JSON

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(POST_URL))
          .timeout(Duration.ofSeconds(10)) // Adjusted to 10 seconds for timeout
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
