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
  public static final int INITIAL_THREADS = 32;
  public static final int REQUESTS_PER_THREAD_INITIAL = 1000;  // Each initial thread sends 1000 requests
  public static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(TOTAL_REQUESTS * 4);
  public static final AtomicInteger successfulRequests = new AtomicInteger(0);
  public static final AtomicInteger failedRequests = new AtomicInteger(0);
  private static volatile boolean firstThreadCompleted = false; // Flag to indicate first thread completion

  public static void main(String[] args) {
    // Start request generation
    Thread eventGeneratorThread = new Thread(new EventGenerator());
    eventGeneratorThread.start();

    // Latch to wait for the first thread to complete
    CountDownLatch latch = new CountDownLatch(1);

    long startTime = System.currentTimeMillis();

    // Create ThreadPoolExecutor
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        INITIAL_THREADS,
        INITIAL_THREADS,
        5000L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(REQUESTS_PER_THREAD_INITIAL * INITIAL_THREADS),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    for (int i = 0; i < INITIAL_THREADS; i++) {
      executor.execute(new PostingRequestThread(latch));
    }

    // Wait for the first thread to finish
    try {
      latch.await(); // Wait for one thread to finish
    } catch (InterruptedException e) {
      System.out.println("Main thread was interrupted: " + e.getMessage());
      Thread.currentThread().interrupt();
    }

    // Print results
    long totalTime = System.currentTimeMillis() - startTime;
    System.out.println("Successful requests: " + successfulRequests.get());
    System.out.println("Failed requests: " + failedRequests.get());
    System.out.println("Total time: " + totalTime + " ms");
    System.out.println("Throughput: " + (successfulRequests.get() / (totalTime / 1000.0)) + " requests/second");

    executor.shutdown(); // Shutdown the executor
    System.exit(0); // Terminate the program
  }

  public static BlockingQueue<LiftRideEvent> getEventQueue() {
    return eventQueue;
  }

  public static AtomicInteger getSuccessfulRequests() {
    return successfulRequests;
  }

  public static AtomicInteger getFailedRequests() {
    return failedRequests; // Fix this line
  }

  public static boolean isFirstThreadCompleted() {
    return firstThreadCompleted;
  }

  public static void setFirstThreadCompleted(boolean completed) {
    firstThreadCompleted = completed;
  }
}
