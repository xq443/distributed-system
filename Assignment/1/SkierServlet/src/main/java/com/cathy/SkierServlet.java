package com.cathy;

import com.cathy.bean.RequestData;
import com.cathy.bean.ResponseData;
import com.google.gson.Gson;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/")
public class SkierServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Gson gson = new Gson();
    response.setContentType("application/json");

    // Read and handle JSON Request
    StringBuilder bodyBuilder = new StringBuilder();
    String temp;
    while ((temp = request.getReader().readLine()) != null) {
      bodyBuilder.append(temp);
    }

    try {
      // Parse the JSON body into a RequestData object
      RequestData requestData = gson.fromJson(bodyBuilder.toString(), RequestData.class);

      // Validate missing parameters
      String missingParams = areParametersMissing(requestData);
      if (!missingParams.isEmpty()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400: Bad Request
        //response.getWriter().write("Missing parameters: " + missingParams);
        response.getOutputStream().print(gson.toJson(new ResponseData("Missing parameters: " + missingParams)));
        return;
      }

      // Validate parameter values
      String invalidParams = areParametersValid(requestData);
      if (!invalidParams.isEmpty()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400: Invalid inputs
        //response.getWriter().write("Invalid inputs: " + invalidParams);
        response.getOutputStream().print(gson.toJson(new ResponseData("Invalid inputs: " + invalidParams)));
        return;
      }

      response.setStatus(HttpServletResponse.SC_CREATED); // 201: Done, and created
      response.getOutputStream().print(gson.toJson(requestData));

    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        //response.getWriter().write("Web Server Error: " + e.getMessage());
        response.getOutputStream().print(gson.toJson(new ResponseData("Web Server Error: " + e.getMessage())));
    } finally {
      response.getOutputStream().flush();
    }
  }

  // Helper method to check if input parameters are missing
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

    return missingParams.toString().isEmpty() ? "" : missingParams.substring(0, missingParams.length() - 2); // Remove last comma and space
  }

  // Helper method to validate the input parameter values
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

    return invalidParams.toString().isEmpty() ? "" : invalidParams.substring(0, invalidParams.length() - 2); // Remove last comma and space
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Gson gson = new Gson();
    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    //response.getWriter().write("GET method is not supported in this assignment. Please use POST.");
    response.getOutputStream().print(gson.toJson(new ResponseData("GET method is not supported in this assignment. Please use POST.")));
  }
}
