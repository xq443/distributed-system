# CS6650 Lab 4 - Playing with Sockets

### Aims: 
1. Experiment with a multithreaded socket server
1. Understand barrier synchronization witha CyclicBarrier 

#### Run the Socket Server
1. Copy and build the code from [week 3](https://github.com/gortonator/bsds-6650/tree/master/code/week-3)
1. Run SocketServer.java and leave it running - it waits forever for requests
1. Run SocketClientSingleThreaded.java. This should send a message to your server. Check the output window to ensure it works
1. Look at the code in these files to make sure you understand what is going on

#### Complete the Socket Client
1. Open SocketClientMultithreaded.java and read the code
1. The // TO DO comments point you to where you need to insert code to initialize the CyclicBarrier, create the client threads and implement the barrier synchronization
1. You also need to insert the CyclicBarrier handling into SocketClientThread.java
1. Run the code. You should see multiple requests being processed by the server. 
1. Run this test a few times. What is the maximum numbers of active servers you see?

#### Modify the server to use a thread pool
1. The server right now creates a thread per request. What do you think will happen if you create 100's or 1000's of simultaneous clients? Your laptop might not have enough resources to test this, but you should be able to guess :)
1. Create a new server that utilizes a FIXED SIZE thread pool. Make a sensible guess how large this pool should be (e.g. 20?)
1. In the client, tale two timestamps, one before any threads run and one after all threads complete. Print out the wall time (test duration) in milliseconds before exiting
1. Experiment with different nimbers of clients and thread pool sizes. Do you see much variation in the wall time for a test?
1. Compare the performance of the system with the original server and your modified server? Is there a noticable difference in performance?

#### Modify the code to use UDPs
1. Look at the UDP examples [here](https://www.baeldung.com/udp-in-java) and create a new version of your client-server that utilizes datagrams
1. Compare the wall times for the two versions. Do you see a significant difference?


If you want to look at some solutions, they are [here](https://github.com/gortonator/bsds-6650/tree/master/code/week-3/solutions)

If you want to know more, a good comprehensive overview of Java sockets programming is [here](https://www.baeldung.com/a-guide-to-java-sockets). 
Armed with this knowledge you could for example modify the client and server to reuse a single socket to send a series of requests rather than creating a new socket each time.