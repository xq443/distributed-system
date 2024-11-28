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

@WebServlet("/skiers/*")
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
      String missingParams = areParametersMissing(requestData);
      if (!missingParams.isEmpty()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getOutputStream()
                .print(gson.toJson(new ResponseData("Missing parameters: " + missingParams)));
        return;
      }

      // Validate parameter values
      String invalidParams = areParametersValid(requestData);
      if (!invalidParams.isEmpty()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getOutputStream()
                .print(gson.toJson(new ResponseData("Invalid inputs: " + invalidParams)));
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


  private String areParametersMissing(RequestData requestData) {
    StringBuilder missingParams = new StringBuilder();

    if (requestData == null) {
      return "requestData";
    }
    if (requestData.getSkierID() == null) missingParams.append("skierID, ");
    if (requestData.getResortID() == null) missingParams.append("resortID, ");
    if (requestData.getLiftID() == null) missingParams.append("liftID, ");
    if (requestData.getSeasonID() == null) missingParams.append("seasonID, ");
    if (requestData.getDayID() == null) missingParams.append("dayID, ");
    if (requestData.getTime() == null) missingParams.append("time, ");

    return missingParams.toString().isEmpty() ? "" : missingParams.substring(0, missingParams.length() - 2);
  }

  private String areParametersValid(RequestData requestData) {
    StringBuilder invalidParams = new StringBuilder();

    if (requestData.getSkierID() < 1 || requestData.getSkierID() > 100000) {
      invalidParams.append("skierID, ");
    }
    if (requestData.getResortID() < 1 || requestData.getResortID() > 10) {
      invalidParams.append("resortID, ");
    }
    if (requestData.getLiftID() < 1 || requestData.getLiftID() > 40) {
      invalidParams.append("liftID, ");
    }
    if (!"2024".equals(requestData.getSeasonID())) {
      invalidParams.append("seasonID, ");
    }
    if (!"1".equals(requestData.getDayID())) {
      invalidParams.append("dayID, ");
    }
    if (requestData.getTime() < 1 || requestData.getTime() > 360) {
      invalidParams.append("time, ");
    }

    return invalidParams.toString().isEmpty() ? "" : invalidParams.substring(0, invalidParams.length() - 2);
  }

  @Override
  public void destroy() {
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

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("{\"message\": \"Missing parameters\"}");
      return;
    }

    String[] urlParts = urlPath.split("/");

    try {
      if (urlParts.length == 8 && urlParts[6].equals("skiers") && isUrlValid(urlParts)) {
        // /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
        getSkierID(response, urlParts);
      } else if (urlParts.length == 3 && urlParts[2].equals("vertical") && isUrlValid(urlParts)) {
        // /skiers/{skierID}/vertical
        getSkierVertical(response, urlParts);
      } else {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("{\"message\": \"Invalid URL path\"}");
      }
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"message\": \"Internal server error\"}");
      e.printStackTrace();
    }
  }

  private boolean isUrlValid(String[] urlPath) {
    try {
      if (urlPath.length == 8 && urlPath[6].equals("skiers")) {
        int resortID = Integer.parseInt(urlPath[1]);
        int skierID = Integer.parseInt(urlPath[7]);
        int seasonID = Integer.parseInt(urlPath[3]);
        return resortID == 1 && skierID >= 1 && skierID <= 100000 && seasonID == 2024;
      } else if (urlPath.length == 3 && urlPath[2].equals("vertical")) {
        int skierID = Integer.parseInt(urlPath[1]);
        return skierID >= 1 && skierID <= 100000;
      }
    } catch (NumberFormatException e) {
      return false;
    }
    return false;
  }

  private void getSkierID(HttpServletResponse response, String[] urlParts) throws IOException {
    try (Jedis jedis = jedisPool.getResource()) {
      int resortID = Integer.parseInt(urlParts[1]);
      String seasonID = urlParts[3];
      String dayID = urlParts[5];
      int skierID = Integer.parseInt(urlParts[7]);


      String redisKey = String.format("skier:%d:resort:%d:season:%s:day:%s", skierID, resortID, seasonID, dayID);
      List<String> liftRides = jedis.lrange(redisKey, 0, -1);

      if (liftRides.isEmpty()) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("{\"message\": \"No data found for the skier on the given day\"}");
        return;
      }
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(new Gson().toJson(liftRides));
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid skierID or dayID\"}");
    }
  }

  private void getSkierVertical(HttpServletResponse response, String[] urlParts) throws IOException {
    try (Jedis jedis = jedisPool.getResource()) {
      int skierID = Integer.parseInt(urlParts[1]);

      String pattern = String.format("skier:%d:resort:*:season:*:day:*", skierID);
      Set<String> keys = jedis.keys(pattern);

      int totalVertical = 0;

      for (String key : keys) {
        List<String> rides = jedis.lrange(key, 0, -1);
        for (String ride : rides) {
          RequestData liftRide = new Gson().fromJson(ride, RequestData.class);
          totalVertical += liftRide.getLiftID() * 10;
        }
      }

      if (totalVertical == 0) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("{\"message\": \"No data found for the skier\"}");
        return;
      }

      Map<String, Object> result = new HashMap<>();
      result.put("skierID", skierID);
      result.put("totalVertical", totalVertical);

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(new Gson().toJson(result));
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"message\": \"Invalid skierID\"}");
    }
  }

}
