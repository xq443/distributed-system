package consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.*;

public class LiftRideConsumer {

  private static final String QUEUE_NAME = "SkierQueue";
  private static final int THREAD_POOL_SIZE = 280;
  private static final int CHANNEL_POOL_SIZE = 200; // Define size of the channel pool
  private static final ConnectionFactory factory = new ConnectionFactory();
  private static final Gson gson = new Gson();
  private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

  // Thread-safe map to store lift rides by skierID
  private static final ConcurrentMap<Integer, ConcurrentLinkedQueue<LiftRide>> skierRidesMap = new ConcurrentHashMap<>();

  // Channel pool
  private static final BlockingQueue<Channel> channelPool = new LinkedBlockingQueue<>(CHANNEL_POOL_SIZE);

  public static void main(String[] args) {
    factory.setHost("34.221.69.60");
    //factory.setRequestedHeartbeat(60); // Set heartbeat to 60 seconds

    try (Connection connection = factory.newConnection()) {
      initChannelPool(connection); // Initialize channel pool with the connection

      for (int i = 0; i < THREAD_POOL_SIZE; i++) {
        executorService.submit(() -> {
          try {
            Channel channel = channelPool.take(); // Get a channel from the pool
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicConsume(QUEUE_NAME, true, getConsumer(channel));
          } catch (IOException | InterruptedException e) {
            e.printStackTrace();
          }
        });
      }

      // Add a shutdown hook to gracefully terminate the executor service
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        executorService.shutdown();
        System.out.println("Consumer application terminated.");
      }));

      // Prevent the main thread from exiting immediately
      executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException("Error initializing RabbitMQ connection: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void initChannelPool(Connection connection) throws IOException {
    // Create and add channels to the pool using the provided connection
    for (int i = 0; i < CHANNEL_POOL_SIZE; i++) {
      Channel channel = connection.createChannel();
      channelPool.offer(channel);
    }
    System.out.println("Channel pool initialized with " + CHANNEL_POOL_SIZE + " channels.");
  }

  private static Consumer getConsumer(Channel channel) {
    return new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        // Convert the body to a string
        String message = new String(body);

        // Deserialize the JSON payload to LiftRide object
        LiftRide liftRide = gson.fromJson(message, LiftRide.class);

        // Store the lift ride data for each skier in a thread-safe way
        skierRidesMap.computeIfAbsent(liftRide.getSkierID(), k -> new ConcurrentLinkedQueue<>()).add(liftRide);

        // Log the successful processing of the lift ride
        //System.out.println("Received and recorded lift ride for SkierID: " + liftRide.getSkierID());
      }
    };
  }
}
