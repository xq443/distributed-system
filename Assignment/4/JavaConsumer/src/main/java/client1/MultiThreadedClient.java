package client1;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadedClient {
    private static final int NUM_THREADS = 200;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int TOTAL_REQUESTS = 200000;
    private static AtomicInteger successfulRequests = new AtomicInteger();
    private static AtomicInteger failedRequests = new AtomicInteger();


    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>();
        Thread eventGeneratorThread = new Thread(new EventGenerator(eventQueue));
        eventGeneratorThread.start();
        eventGeneratorThread.join();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        CountDownLatch countDownLatch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(new RunThread(countDownLatch, eventQueue, REQUESTS_PER_THREAD));
        }

        countDownLatch.await();

        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long wallTime = (endTime - startTime) / 1000;

        System.out.println("Number of successful requests: " + successfulRequests);
        System.out.println("Number of failed requests: " + failedRequests);
        System.out.println("Total run time: " + wallTime + " seconds");
        System.out.println("Throughput: " + (TOTAL_REQUESTS / wallTime) + " requests/second");

    }


    static class RunThread implements Runnable {
        private final BlockingQueue<LiftRideEvent> eventQueue;
        private final CountDownLatch countDownLatch;

        private final int maxRequests;
        public RunThread(CountDownLatch countDownLatch, BlockingQueue<LiftRideEvent> eventQueue, int maxRequests) {
            this.countDownLatch = countDownLatch;
            this.eventQueue = eventQueue;
            this.maxRequests = maxRequests;
        }

        @Override
        public void run() {
            ApiClient apiClient = new ApiClient();
            apiClient.setBasePath("http://35.167.108.79:8080/JavaServlets_war/");
            //apiClient.setBasePath("http://localhost:8080/JavaServlets_war_exploded");
            apiClient.setConnectTimeout(10000);
            apiClient.setReadTimeout(10000);

            SkiersApi skiersApi = new SkiersApi(apiClient);
            int requestsSent = 0;
            while (requestsSent < maxRequests && !eventQueue.isEmpty()) {
                LiftRideEvent event;
                try {
                    event = eventQueue.poll(500, TimeUnit.MILLISECONDS);  // Timeout to avoid blocking
                    if (event == null) break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                try {
                    skiersApi.writeNewLiftRideWithHttpInfo(event.getLiftRide(), event.getResortID(),
                            event.getSeasonID(), event.getDayID(), event.getSkierID());
                    incrementSuccessCount();
                } catch (ApiException e) {
                    incrementFailureCount();
                    System.err.println("Failed request: " + e.getMessage());
                    e.printStackTrace();
                }
                requestsSent++;

            }
            countDownLatch.countDown();

        }
        private void incrementSuccessCount() {
            successfulRequests.incrementAndGet();
        }

        private void incrementFailureCount() {
            failedRequests.incrementAndGet();
        }
    }

}
