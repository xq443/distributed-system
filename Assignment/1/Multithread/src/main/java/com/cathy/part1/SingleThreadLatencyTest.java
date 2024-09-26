package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import com.google.gson.Gson;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;

public class SingleThreadLatencyTest {
  private static final int TOTAL_REQUESTS = 10000;

  public static void main(String[] args) {
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < TOTAL_REQUESTS; i++) {
      LiftRideEvent event = generateLiftRideEvent(i); // Generate a LiftRideEvent
      int responseCode = sendRequest(event);
      if (responseCode != HttpServletResponse.SC_CREATED) {
        System.out.println("Request " + (i + 1) + " failed with response code: " + responseCode);
      }
    }

    long totalTime = System.currentTimeMillis() - startTime;

    System.out.println("Total time for " + TOTAL_REQUESTS + " requests: " + totalTime + " ms");
    System.out.printf("Average latency per request: %.2f ms%n", (totalTime / (double) TOTAL_REQUESTS));
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
    Gson gson = new Gson();
    String jsonInputString = gson.toJson(event); // Convert the LiftRideEvent object to JSON

    try {
      URL url = new URL("http://localhost:8080/SkierServlet_war_exploded/skiers");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setDoOutput(true);

      try (var os = conn.getOutputStream()) {
        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      // Log the response code and potentially the error message if needed
      return conn.getResponseCode();
    } catch (Exception e) {
      System.out.println("Error sending request: " + e.getMessage());
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // Simulate a server error for testing
    }
  }
}
