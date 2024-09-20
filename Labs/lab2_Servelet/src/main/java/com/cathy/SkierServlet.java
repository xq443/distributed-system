package com.cathy;

import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.util.regex.Pattern;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import com.google.gson.Gson;

@WebServlet("/")
public class SkierServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        // Check if we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("Missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");

        // Validate URL path and return the response status code
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("Invalid URL");
            return;
        }

        // Process the URL parameters if valid
        String skierId = urlParts[1];
        String seasonId = urlParts[3];
        String dayId = urlParts[5];
        String skier = urlParts[7];

        // Mock processing logic, in real case, data should be fetched from a database or service
        if (skierId.equals("123") && seasonId.equals("2019") && dayId.equals("1") && skier.equals("123")) {
            // If the URL params match a known skier/lift ride, return some mock data
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("{\"message\":\"Skier data found\", \"skierId\":\"123\", \"seasonId\":\"2019\", \"dayId\":\"1\", \"skier\":\"123\"}");
        } else {
            // If no matching data is found, return a 404
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("{\"message\":\"Data not found for the given skier or season\"}");
        }
    }

    private boolean isUrlValid(String[] urlPath) {
        return urlPath.length == 8 &&
            isNumeric(urlPath[1]) &&
            "seasons".equals(urlPath[2]) &&
            isNumeric(urlPath[3]) &&
            "days".equals(urlPath[4]) &&
            isNumeric(urlPath[5]) &&
            "skier".equals(urlPath[6]) &&
            isNumeric(urlPath[7]);
    }

    private boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        return pattern.matcher(str).matches();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();

        // Check if the URL is present
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing parameters");
            return;
        }

        // Split the URL path and validate
        String[] urlParts = urlPath.split("/");
        if (!isUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Invalid URL");
            return;
        }

        // Read and parse the JSON request body
        StringBuilder bodyBuilder = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        }

        String requestBody = bodyBuilder.toString();

        // Parse JSON into a POJO
        try {
            SkierLiftRide liftRide = gson.fromJson(requestBody, SkierLiftRide.class);

            // For this example, just return the parsed data back
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(liftRide));
            response.getWriter().write("POST request processed successfully!");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid JSON format");
        }
    }
}
