import com.rabbitmq.client.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;
import io.swagger.client.model.LiftRide;

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {

    private static final String QUEUE_NAME = "skiersQueue";
    private Connection connection;
    private Channel channel;
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setUsername("guest");
            factory.setPassword("guest");
            //factory.setHost("35.82.187.36");
//            factory.setUsername("veratao");
//            factory.setPassword("password");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        } catch (Exception e) {
            throw new ServletException("Failed to initialize RabbitMQ connection", e);
        }
    }
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");


        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // TODO: process url params in `urlParts`
            res.getWriter().write("It works!");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        // Check if we have a valid URL path
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("Missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");

        // Validate the URL path
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("{\"message\":\"Invalid URL path\"}");
            return;
        }

        int resortID = Integer.parseInt(urlParts[1]);
        String seasonID = urlParts[3];
        String dayID = urlParts[5];
        int skierID = Integer.parseInt(urlParts[7]);

        Map<String, Object> headers = Map.of(
                "resortID", resortID,
                "seasonID", seasonID,
                "dayID", dayID,
                "skierID", skierID
        );

        // Process the JSON request body
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
        }
        Gson gson = new Gson();

        String jsonRequestBody = jsonBuilder.toString();
        LiftRide liftRide = gson.fromJson(jsonRequestBody,LiftRide.class);
        int liftID = liftRide.getLiftID();
        int time = liftRide.getTime();

        if (liftID < 1 || liftID > 40 || time < 1 || time > 360) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"message\": \"Invalid liftID or time\"}");
            return;
        }

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().headers(headers).build();

        try {
            String message = jsonRequestBody;
            channel.basicPublish("", QUEUE_NAME, properties, message.getBytes(StandardCharsets.UTF_8));
            res.setStatus(HttpServletResponse.SC_CREATED);
            res.getWriter().write("{\"message\": \"Lift ride processed and sent to queue\"}");

        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"message\": \"Failed to send message to queue\"}");
            e.printStackTrace();
        }
    }



    private boolean isUrlValid(String[] urlPath) {
        ///{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
        // urlPath  = "/1/seasons/2019/days/1/skiers/123"
        // urlParts = [, 1, seasons, 2019, days, 1, skiers, 123]

        if (urlPath.length != 8) {
            return false;
        }
        try {
            int resortID = Integer.parseInt(urlPath[1]);
            int skierID = Integer.parseInt(urlPath[7]);


            if (skierID < 1 || skierID > 100000 || resortID < 1 || resortID > 10) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return urlPath[2].equals("seasons") &&
                urlPath[4].equals("days") && urlPath[6].equals("skiers");

    }
}
