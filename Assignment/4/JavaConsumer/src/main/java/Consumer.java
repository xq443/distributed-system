import com.google.gson.Gson;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.swagger.client.model.LiftRide;
import redis.clients.jedis.Jedis;

public class Consumer {

    private static final String QUEUE_NAME = "skiersQueue";
    private static ConcurrentHashMap<Integer, String> skierMap = new ConcurrentHashMap<>();
    private static Jedis redis;

    public static void main(String[] argv) throws Exception {
        redis = new Jedis("localhost", 6379);

        ConnectionFactory factory = new ConnectionFactory();
        //factory.setHost("localhost");
        //factory.setUsername("guest");
        //factory.setPassword("guest");
        factory.setHost("35.82.187.36");  //rabbitmq instance ip
        factory.setUsername("veratao");
        factory.setPassword("password");

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" - Waiting for messages.");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" Received '" + message + "'");

            LiftRide liftRide = new Gson().fromJson(message, LiftRide.class);
            if (liftRide == null) {
                System.err.println("Invalid liftRide data, skipping processing.");
                return;
            }

            Map<String, Object> headers = delivery.getProperties().getHeaders();
            if (headers == null) {
                System.err.println("Headers are missing, skipping processing.");
                return;
            }
            try{
                int resortID = (int) headers.get("resortID");
                String seasonID = headers.get("seasonID").toString();
                String dayID = headers.get("dayID").toString();
                int skierID = (int) headers.get("skierID");
                passLiftRideEvent(liftRide, skierID, resortID, seasonID, dayID);
            } catch (Exception e) {
                System.err.println("Error processing headers: " + e.getMessage());
            }

        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});

        executorService.submit(() -> {
            while (true) {
                Thread.sleep(1000);
            }
        });
    }

    public static void passLiftRideEvent(LiftRide liftRide, int skierID, int resortID, String seasonID, String dayID) {
        int liftID = liftRide.getLiftID();
        int vertical = liftID * 10;

        redis.sadd("skier:" + skierID + ":days", dayID);
        redis.hincrBy("skier:" + skierID + ":vertical:" + dayID, "total", vertical);
        redis.lpush("skier:" + skierID + ":lifts:" + dayID, String.valueOf(liftID));
        redis.sadd("resort:" + resortID + ":day:" + dayID + ":skiers", String.valueOf(skierID));

        System.out.println("Processed LiftRideEvent for skierID: " + skierID + ", resortID: " + resortID);
    }

}

