package com.cathy;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@WebServlet("/api/*")
public class SkierServlet extends HttpServlet {

  private final static String QUEUE_NAME = "SkierQueue";
  private static final ConnectionFactory factory = new ConnectionFactory();
  private static final int CHANNEL_POOL_SIZE = 120;
  private static final BlockingQueue<Channel> channelPool = new LinkedBlockingQueue<>(CHANNEL_POOL_SIZE);
  private final Gson gson = new Gson();
  private static Connection connection;

  private static final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);


  @Override
  public void init() {
    //factory.setHost("54.186.130.49"); // rabbitmq
    factory.setHost("localhost");
    //factory.setConnectionTimeout(500000); // Set timeout to 50 seconds
    try {
      connection = factory.newConnection();
      for (int i = 0; i < CHANNEL_POOL_SIZE; i++) {
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channelPool.add(channel);
      }
      System.out.println("Connection to RabbitMQ established.");
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException("Error initializing RabbitMQ connection: " + e.getMessage(), e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("application/json");

    // System.out.println("Received request body: " + bodyBuilder); // Log the request body
    StringBuilder bodyBuilder = new StringBuilder();
    String line;
    while ((line = request.getReader().readLine()) != null) {
      bodyBuilder.append(line);
    }

    try {
      // Parse the JSON body into a RequestData object
      RequestData requestData = gson.fromJson(bodyBuilder.toString(), RequestData.class);

      // Validate missing parameters
      if (areParametersMissing(requestData)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getOutputStream()
                .print(gson.toJson(new ResponseData("Missing parameters")));
        return;
      }

      // Validate parameter values
      if (!areParametersValid(requestData)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getOutputStream()
                .print(gson.toJson(new ResponseData("Invalid inputs: check the values out of boundary")));
        return;
      }

      // Package message and send to RabbitMQ
      String message = packageMessage(requestData);

      // Send data to RabbitMQ message queue
      sendToMessageQueue(message);
      response.setStatus(HttpServletResponse.SC_CREATED);
      response.getOutputStream().print(gson.toJson(requestData));

    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getOutputStream()
          .print(gson.toJson(new ResponseData("Web Server Error: " + e.getMessage())));
    } finally {
      response.getOutputStream().flush();
    }
  }

  private void sendToMessageQueue(String message) {
    Channel channel = channelPool.poll();
    if (channel == null) {
      throw new RuntimeException("No available channels in the pool.");
    }
    try {
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
      //System.out.println("'" + message + "'");
    } catch (IOException e) {
      throw new RuntimeException("Error sending message to RabbitMQ: " + e.getMessage(), e);
    } finally {
      channelPool.offer(channel);
    }
  }

  private String packageMessage(RequestData requestData) {
    return "{"
            + "\"skierID\":\"" + requestData.getSkierID() + "\","
            + "\"resortID\":\"" + requestData.getResortID() + "\","
            + "\"seasonID\":\"" + requestData.getSeasonID() + "\","
            + "\"dayID\":\"" + requestData.getDayID() + "\","
            + "\"time\":\"" + requestData.getTime() + "\","
            + "\"liftID\":\"" + requestData.getLiftID() + "\""
            + "}";
  }

  // Get request handler
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Missing URL path\"}");
      return;
    }

    String[] urlParts = urlPath.split("/");

    // Handle GET /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
    if (urlParts.length == 8 && "seasons".equals(urlParts[3]) && "day".equals(urlParts[5])) {
      handleUniqueSkiersNumber(request, response, urlParts);
    }
    // Handle GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
    else if (urlParts.length == 9 && "seasons".equals(urlParts[3]) && "days".equals(urlParts[5]) && ("skiers".equals(urlParts[1]) || "skiers".equals(urlParts[7]))) {
      handleTotalVerticalSkierForSpecificDay(request, response, urlParts);

      // Handle /skiers/{skierID}/vertical
    } else if (urlParts.length == 4 && "skiers".equals(urlParts[1]) && "vertical".equals(urlParts[3])) {
      handleTotalVertical(request, response, urlParts);
    }
    else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid URL format\"}");
    }
  }

  // Handle GET /skiers/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
  // Test url: http://localhost:8080/SkierServlet_war_exploded/resorts/2/seasons/2024/day/1/skiers/200
  private void handleUniqueSkiersNumber(HttpServletRequest request, HttpServletResponse response, String[] urlParts) throws IOException {
    try {
      Integer resortID = Integer.parseInt(urlParts[2]);
      String seasonID = urlParts[4];
      String dayID = urlParts[6];

      if (!isValidResortID(resortID) || !isValidSeasonID(seasonID) || !isValidDayID(dayID)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"message\": \"Invalid parameters\"}");
        return;
      }

      try (Jedis jedis = jedisPool.getResource()) {
        String key = String.format("resort:%s:season:%s:day:%s", resortID, seasonID, dayID);
        Set<String> skierIDs = jedis.smembers(key);

        if (skierIDs == null || skierIDs.isEmpty()) {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          response.getWriter().write("{\"message\": \"No skiers found for the given parameters\"}");
        } else {
          Map<String, Object> responseData = new HashMap<>();
          responseData.put("numSkiers", skierIDs.size());
          response.setStatus(HttpServletResponse.SC_OK);
          response.getWriter().write(gson.toJson(responseData));
        }
      }

    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"message\": \"Server error: " + e.getMessage() + "\"}");
    }
  }

  // Handle GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
  // Test url: http://localhost:8080/SkierServlet_war_exploded/skiers/2/seasons/2024/day/1/skiers/200
  private void handleTotalVerticalSkierForSpecificDay(HttpServletRequest request, HttpServletResponse response, String[] urlParts) throws IOException {
    try {
      Integer resortID = Integer.parseInt(urlParts[2]);
      String seasonID = urlParts[4];
      String dayID = urlParts[6];
      Integer skierID = Integer.parseInt(urlParts[8]);

      if (!isValidResortID(resortID) || !isValidSeasonID(seasonID) || !isValidDayID(dayID) || !isValidSkierID(skierID)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"message\": \"Invalid parameters\"}");
        return;
      }

      // Fetch skier's lift ride data for the given day
      try (Jedis jedis = jedisPool.getResource()) {
        String key = String.format("resort:%s:season:%s:day:%s:skier:%s", resortID, seasonID, dayID, skierID);
        // list type
        List<String> liftRides = jedis.lrange(key, 0, -1);

        if (liftRides == null || liftRides.isEmpty()) {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          response.getWriter().write("{\"message\": \"No lift rides found for the specified skier\"}");
        } else {
          int totalVertical = 0;

          for (String liftRide : liftRides) {
            // calculated as liftID * 10
            int liftID = Integer.parseInt(liftRide);
            int verticalForThisRide = liftID * 10;
            totalVertical += verticalForThisRide;
          }

          // response
          Map<String, Object> responseData = new HashMap<>();
          responseData.put("skierID", skierID);
          responseData.put("resortID", resortID);
          responseData.put("seasonID", seasonID);
          responseData.put("dayID", dayID);
          responseData.put("totalVertical", totalVertical);
          response.setStatus(HttpServletResponse.SC_OK);
          response.getWriter().write(gson.toJson(responseData));
        }
      }

    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"message\": \"Server error: " + e.getMessage() + "\"}");
    }
  }

  // Handle GET /skiers/{skierID}/vertical
  // Test url: http://localhost:8080/SkierServlet_war_exploded/skiers/200/vertical
  private void handleTotalVertical(HttpServletRequest request, HttpServletResponse response, String[] urlParts) throws IOException {
    try {
      Integer skierID = Integer.parseInt(urlParts[2]);

      // Validate skierID
      if (!isValidSkierID(skierID)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"message\": \"Invalid skierID\"}");
        return;
      }

      try (Jedis jedis = jedisPool.getResource()) {
        // wildcard pattern to match keys for the skier across all resorts, seasons, and days
        String pattern = String.format("resort:*:season:*:day:*:skier:%s", skierID);

        Set<String> matchingKeys = jedis.keys(pattern); // Fetch all matching keys
        if (matchingKeys == null || matchingKeys.isEmpty()) {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          response.getWriter().write("{\"message\": \"No data found for the specified skier\"}");
          return;
        }

        int totalVertical = 0;

        for (String key : matchingKeys) {
          List<String> liftRides = jedis.lrange(key, 0, -1);
          for (String liftRide : liftRides) {
            int liftID = Integer.parseInt(liftRide);
            int verticalForThisRide = liftID * 10;
            totalVertical += verticalForThisRide;
          }
        }

        // response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("skierID", skierID);
        responseData.put("totalVertical", totalVertical);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(responseData));
      }

    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"message\": \"Server error: " + e.getMessage() + "\"}");
    }
  }

  @Override
  public void destroy() {
    // Close JedisPool to clean up any background threads
    jedisPool.close();

    // Close each channel in the channel pool
    for (Channel channel : channelPool) {
      try {
        if (channel.isOpen()) {
          channel.close();
        }
      } catch (IOException | TimeoutException e) {
        System.err.println("Failed to close channel: " + e.getMessage());
      }
    }

    // Close the RabbitMQ connection
    try {
      if (connection != null && connection.isOpen()) {
        connection.close();
      }
    } catch (IOException e) {
      System.err.println("Failed to close RabbitMQ connection: " + e.getMessage());
    }

    super.destroy();
  }

  // Helper methods for parameter validation
  private boolean areParametersMissing(RequestData requestData) {
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

  private boolean areParametersValid(RequestData requestData) {
    return isValidSkierID(requestData.getSkierID()) &&
        isValidResortID(requestData.getResortID()) &&
        isValidLiftID(requestData.getLiftID()) &&
        isValidSeasonID(requestData.getSeasonID()) &&
        isValidDayID(requestData.getDayID()) &&
        isValidTimeID(requestData.getTime());
  }


  private boolean isValidResortID(Integer resortID) {
    try {
      return resortID >= 1 && resortID <= 10;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isValidSeasonID(String seasonID) {
    return "2024".equals(seasonID);
  }

  private boolean isValidDayID(String dayID) {
    try {
      int id = Integer.parseInt(dayID);
      return id == 1;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isValidSkierID(Integer skierID) {
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
