package consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import redis.clients.jedis.Jedis;
import java.util.Map;
import java.util.concurrent.*;

public class LiftRideConsumer {

  private static final String QUEUE_NAME = "SkierQueue";
  private static Jedis redis;

  public static void main(String[] argv) throws Exception {
    // Initialize Redis connection
    redis = new Jedis("54.202.226.237", 6379);

    // Setup RabbitMQ connection factory
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("34.210.18.112");  // RabbitMQ instance IP

    // Create executor service for concurrent task handling
    ExecutorService executorService = Executors.newFixedThreadPool(100);

    // Establish RabbitMQ connection and channel
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    System.out.println(" - Waiting for messages.");

    // Deliver callback with manual acknowledgment
    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      //System.out.println("Received: " + message);

      // Deserialize message into LiftRide object
      LiftRide liftRide = new Gson().fromJson(message, LiftRide.class);
      if (liftRide == null) {
        System.err.println("Invalid LiftRide data, skipping processing.");
        return;
      }

      // Extract headers from the message
      Map<String, Object> headers = delivery.getProperties().getHeaders();
      if (headers == null) {
        System.err.println("Headers are missing, skipping processing.");
        return;
      }

      try {
        // Extract specific headers for processing
        int resortID = (int) headers.get("resortID");
        String seasonID = headers.get("seasonID").toString();
        String dayID = headers.get("dayID").toString();
        int skierID = (int) headers.get("skierID");

        // Process the message into Redis
        passLiftRideEvent(liftRide, skierID, resortID, seasonID, dayID);
      } catch (Exception e) {
        System.err.println("Error processing headers: " + e.getMessage());
      }
    };

    // Start consuming messages with manual acknowledgment
    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
    });

    // Executor for managing other tasks (can be expanded)
    executorService.submit(() -> {
      while (true) {
        Thread.sleep(1000); // Keeping the executor alive for long-running tasks
      }
    });
  }

  // Method to process LiftRide event and store data in Redis
  public static void passLiftRideEvent(LiftRide liftRide, int skierID, int resortID,
      String seasonID, String dayID) {
    int liftID = liftRide.getLiftID();
    int vertical = liftID * 10;  // Example calculation for vertical

    // Storing skier's daily lift ride data in Redis
    redis.sadd("skier:" + skierID + ":days", dayID);
    redis.hincrBy("skier:" + skierID + ":vertical:" + dayID, "total", vertical);
    redis.lpush("skier:" + skierID + ":lifts:" + dayID, String.valueOf(liftID));
    redis.sadd("resort:" + resortID + ":day:" + dayID + ":skiers", String.valueOf(skierID));

//    System.out.println(
//        "Processed LiftRideEvent for skierID: " + skierID + ", resortID: " + resortID);
  }
}
