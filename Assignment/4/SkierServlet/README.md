- Use the Java 11 HTTP client classes to build the API
- POST
  - local test url: http://localhost:8080/SkierServlet_war_exploded
  - ec2 test url: http://34.221.69.60:8080/SkierServlet_war
    1. 201 Created
    - request:
    ```
    {
      "skierID": 1,          // Valid skierID between 1 and 100000
      "resortID": 2,        // Valid resortID between 1 and 10
      "liftID": 1,          // Valid liftID between 1 and 40
      "seasonID": "2024",   // Fixed to "2024"
      "dayID": "1",         // Fixed to "1"
      "time": 100           // Valid time between 1 and 360
    }
    ```
    
    - response:
    ```
    {
        "resortID": 2,
        "seasonID": "2024",
        "dayID": "1",
        "skierID": 1,
        "time": 100,
        "liftID": 1
    }
    ```
    
    2. missing parameters 400 Bad Request
    
    - request:
    ```
      {
        "skierID": 1,          // Valid skierID between 1 and 100000
        "resortID": 2,        // Valid resortID between 1 and 10
        "liftID": 1,          // Valid liftID between 1 and 40
        "seasonID": "2024",   // Fixed to "2024"         // Fixed to "1"
        "time": 100           // Valid time between 1 and 360
      }
      ```
        
    - response:
      ```
      {
          "message": "Missing parameters: dayID"
      }
      ```
    
    3. invalid input 400 Bad Request
    
    - request:
        ```
        {
          "skierID": 1,          // Valid skierID between 1 and 100000
          "resortID": 2,        // Valid resortID between 1 and 10
          "liftID": 1,          // Valid liftID between 1 and 40
          "seasonID": "2024",   // Fixed to "2024"
          "dayID": "1",         // Fixed to "1"
          "time": 10000           // Valid time between 1 and 360
        }
        ```
        
    - response:
        ```
        {
            "message": "Invalid inputs: time"
        }
        ```
- GET/resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
  - local test url: http://localhost:8080/SkierServlet_war_exploded/resorts/3/seasons/2024/day/1/skiers
  - ec2 test url: http://34.221.69.60:8080/SkierServlet_war/resorts/3/seasons/2024/day/1/skiers
  - Description: get number of unique skiers at resort/season/day
    1. 200 OK
    ```
    {
    "numSkiers": 2
    }
    ```
    2. 400 Bad Request
    ```
    {
    "message": "Invalid parameters"
    }
    ```
    3. 404 Not Found
    ```
    {
    "message": "No skiers found for the given parameters"
    }
    ```
- GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
  - local test url: http://localhost:8080/SkierServlet_war_exploded/skiers/2/seasons/2024/days/1/skiers/100
  - ec2 test url: http://34.221.69.60:8080/SkierServlet_war/skiers/2/seasons/2024/days/1/skiers/100
  - Description: get the total vertical for the skier for the specified ski day
      1. 200 OK
    ```
    {
    "skierID": "100",
    "seasonID": "2024",
    "resortID": "2",
    "dayID": "1",
    "totalVertical": 10
    }
    ```
      2. 400 Bad Request
    ```
    {
    "message": "Invalid parameters"
    }
    ```
      3. 404 Not Found
    ```
    {
    "message": "No lift rides found for the specified skier"
    }
    ```
- GET/skiers/{skierID}/vertical
    - local test url: http://localhost:8080/SkierServlet_war_exploded/skiers/100/vertical
    - ec2 test url: http://34.221.69.60:8080/SkierServlet_war/skiers/100/vertical
    - Description: get the total vertical for the skier for any seasons/days at the any resort
        1. 200 OK
      ```
      {
      "skierID": "300",
      "totalVertical": 20
      }
      ```
        2. 400 Bad Request
      ```
      {
      "message": "Invalid parameters"
      }
      ```
        3. 404 Not Found
      ```
      {
      "message": "No data found for the specified skier"
      }
      ```