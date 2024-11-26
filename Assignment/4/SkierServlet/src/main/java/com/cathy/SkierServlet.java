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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

@WebServlet("/")
public class SkierServlet extends HttpServlet {

  private final static String QUEUE_NAME = "SkierQueue";
  private static final ConnectionFactory factory = new ConnectionFactory();
  private static final int CHANNEL_POOL_SIZE = 150;
  private static final BlockingQueue<Channel> channelPool = new LinkedBlockingQueue<>(CHANNEL_POOL_SIZE);
  private final Gson gson = new Gson();
  private static Connection connection;

  @Override
  public void init() {
    factory.setHost("34.210.18.112"); // rabbitmq
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

    StringBuilder bodyBuilder = new StringBuilder();
    String line;
    while ((line = request.getReader().readLine()) != null) {
      bodyBuilder.append(line);
    }

    // System.out.println("Received request body: " + bodyBuilder); // Log the request body

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

      String skierID = requestData.getSkierID().toString(); // Assuming skierID is of type Integer
      String message = packageMessage(skierID);

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

  private String packageMessage(String skierID) {
    return "{\"skierID\":\"" + skierID + "\"}";
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
    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    response.getOutputStream().print(gson.toJson(new ResponseData("GET method is not supported in this assignment. Please use POST.")));
  }
}
