package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadWorker {
  public static final int TOTAL_REQUESTS = 200000;
  public static final int INITIAL_THREADS = 350;
  public static final int REQUESTS_PER_THREAD = 10;
  private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(TOTAL_REQUESTS * 5);
  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);

  public static void main(String[] args) {
    // Start request generation
    Thread eventGeneratorThread = new Thread(new EventGenerator());
    eventGeneratorThread.start();

    CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

    long startTime = System.currentTimeMillis();

    // Create ThreadPoolExecutor
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        INITIAL_THREADS,
        INITIAL_THREADS,
        5000L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(REQUESTS_PER_THREAD * INITIAL_THREADS),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // Start posting threads immediately: avoiding integer division issues where requests might be left unhandled.
    for (int i = 0; i < (TOTAL_REQUESTS + REQUESTS_PER_THREAD - 1) / REQUESTS_PER_THREAD; i++) {
      executor.execute(new PostingRequestThread(latch));
    }

    // Wait for event generation to complete
    try {
      latch.await(); // Wait for all threads to finish
    } catch (InterruptedException e) {
      System.out.println("Main thread was interrupted: " + e.getMessage());
      Thread.currentThread().interrupt();
    }


    // Print results
    long totalTime = System.currentTimeMillis() - startTime;
    System.out.println("Successful requests: " + successfulRequests.get());
    System.out.println("Failed requests: " + failedRequests.get());
    System.out.println("Total time: " + totalTime + " ms");
    System.out.println("Throughput: " + (TOTAL_REQUESTS / (totalTime / 1000.0)) + " requests/second");
    executor.shutdown();
  }

  public static BlockingQueue<LiftRideEvent> getEventQueue() {
    return eventQueue;
  }

  public static AtomicInteger getSuccessfulRequests() {
    return successfulRequests;
  }

  public static AtomicInteger getFailedRequests() {
    return failedRequests;
  }
}
