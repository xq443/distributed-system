- Use the Java 11 HTTP client classes to build the API
- local test url: http://localhost:8080/SkierServlet_war_exploded/skiers
- ec2 test url: http://34.221.69.60:8080/SkierServlet_war/skiers


1. test case1: 201 Created

request:
`
{
  "skierID": 1,          // Valid skierID between 1 and 100000
  "resortID": 2,        // Valid resortID between 1 and 10
  "liftID": 1,          // Valid liftID between 1 and 40
  "seasonID": "2024",   // Fixed to "2024"
  "dayID": "1",         // Fixed to "1"
  "time": 100           // Valid time between 1 and 360
}
`

response:
`
{
    "resortID": 2,
    "seasonID": "2024",
    "dayID": "1",
    "skierID": 1,
    "time": 100,
    "liftID": 1
}
`


2. test case2: missing parameters 400 Bad Request

request:
`
{
  "skierID": 1,          // Valid skierID between 1 and 100000
  "resortID": 2,        // Valid resortID between 1 and 10
  "liftID": 1,          // Valid liftID between 1 and 40
  "seasonID": "2024",   // Fixed to "2024"         // Fixed to "1"
  "time": 100           // Valid time between 1 and 360
}
`

response:
`
{
    "message": "Missing parameters: dayID"
}
`


3. test case3:  invalid input 400 Bad Request

request:
`
{
  "skierID": 1,          // Valid skierID between 1 and 100000
  "resortID": 2,        // Valid resortID between 1 and 10
  "liftID": 1,          // Valid liftID between 1 and 40
  "seasonID": "2024",   // Fixed to "2024"
  "dayID": "1",         // Fixed to "1"
  "time": 10000           // Valid time between 1 and 360
}
`

response:
 `
{
    "message": "Invalid inputs: time"
}
`
