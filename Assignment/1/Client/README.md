- test how long a single request takes to estimate this latency. 
  - Run a simple test and send eg 10000 requests from a single thread to do this.
- upload all 200K lift ride events to your server as quickly as possible. To do this we'll use multiple threads. The only constraints on your design are as follows:
  - At startup, you must create 32 threads that each send 1000 POST requests and terminate. Once any of these have completed you are free to create as few or as many threads as you like until all the 200K POSTS have been sent. 
  - Lift ride events must be generated in a single dedicated thread and be made available to the threads that make API calls. You need to design this mechanism so that:
    - a posting thread never has to wait for an event to be available. This would slow down your client 
    - it consumes as little CPU and memory as possible, making maximum capacity available for making POST requests 
- The server will return an HTTP 201 response code for a successful POST operation. As soon as the 201 is received, the client thread should immediately send the next request until it has exhausted the number of requests to send.