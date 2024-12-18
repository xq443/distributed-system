package consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.concurrent.*;

public class LiftRideConsumer {

  private static final String QUEUE_NAME = "SkierQueue";
  private static final int THREAD_POOL_SIZE = 80;
  private static final ConnectionFactory factory = new ConnectionFactory();
  private static final Gson gson = new Gson();
  private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

  private static final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "34.220.115.172", 6379); // host same as consumer

  private static Connection connection;
  private static Channel channel;

  public static void main(String[] args) {
    try {
      // Initialize RabbitMQ connection and channel
      factory.setHost("44.244.169.72");
      connection = factory.newConnection();
      channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, false, false, false, null);

      // Submit consumers to thread pool
      for (int i = 0; i < THREAD_POOL_SIZE; i++) {
        executorService.submit(() -> consumeMessages(channel));
      }

      // Add a shutdown hook to clean up resources when the program exits
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        destroy(); // Call the destroy method
        System.out.println("Consumer application terminated.");
      }));

      executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

    } catch (IOException | TimeoutException | InterruptedException e) {
      throw new RuntimeException("Error initializing RabbitMQ connection: " + e.getMessage(), e);
    }
  }

  private static void consumeMessages(Channel channel) {
    try {
      channel.basicConsume(QUEUE_NAME, true, new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
          String message = new String(body);
          // System.out.println("Received message: " + message); // Log the raw JSON
          LiftRide liftRide = gson.fromJson(message, LiftRide.class);

          // Process the message and store it in Redis
          try (Jedis jedis = jedisPool.getResource()) {
            // Calculate vertical from liftID
            int liftVertical = liftRide.getLiftID() * 10; // Vertical = liftID * 10

            // GET/resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
            String redisKey1 = "resort:" + liftRide.getResortID() +
                ":season:" + liftRide.getSeasonID() +
                ":day:" + liftRide.getDayID();
            jedis.sadd(redisKey1, String.valueOf(liftRide.getSkierID()));

            // GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
            String dailyKey = "resort:" + liftRide.getResortID() +
                ":season:" + liftRide.getSeasonID() +
                ":day:" + liftRide.getDayID() +
                ":skier:" + liftRide.getSkierID();
            jedis.lpush(dailyKey, String.valueOf(liftRide.getLiftID()));

            // GET/skiers/{skierID}/vertical
            String verticalKey = "skier:" + liftRide.getSkierID() + ":vertical";
            String seasonField = liftRide.getResortID() + ":" + liftRide.getSeasonID(); // grouped by resort and season
            jedis.hincrBy(verticalKey, seasonField, liftVertical);
          } catch (Exception e) {
            System.err.println("Error processing message for skierID " + liftRide.getSkierID() + ": " + e.getMessage());
          }
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("Error consuming messages: " + e.getMessage(), e);
    }
  }


  private static void destroy() {
    try {
      System.out.println("Shutting down executor service...");
      executorService.shutdown();
      if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }

      System.out.println("Closing RabbitMQ channel...");
      if (channel != null && channel.isOpen()) {
        channel.close();
      }

      System.out.println("Closing RabbitMQ connection...");
      if (connection != null && connection.isOpen()) {
        connection.close();
      }

      System.out.println("Closing Redis pool...");
      jedisPool.close();

    } catch (IOException | TimeoutException | InterruptedException e) {
      System.err.println("Error during shutdown: " + e.getMessage());
    }
  }
}