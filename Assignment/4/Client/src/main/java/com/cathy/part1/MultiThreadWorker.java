package com.cathy.part1;

import com.cathy.bean.LiftRideEvent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadWorker {
  public static final int TOTAL_REQUESTS = 200000;
  public static final int THREADS = 100;
  public static final int REQUESTS_PER_THREAD = 10;

  private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(TOTAL_REQUESTS * 2);
  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);
  private static final CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS); // Latch for total requests

  public static void main(String[] args) {
    // Start request generation
    Thread eventGeneratorThread = new Thread(new EventGenerator());
    eventGeneratorThread.start();

    long startTime = System.currentTimeMillis();

    // Create ThreadPoolExecutor with initial thread pool size
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        THREADS, // Core pool size
        THREADS, // Max pool size (same as core pool size)
        5000L, // Keep-alive time for idle threads
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadPoolExecutor.CallerRunsPolicy() // Strategy for when the queue is full
    );

    // Start initial posting threads
    for (int i = 0; i < TOTAL_REQUESTS / REQUESTS_PER_THREAD; i++) {
      executor.execute(new PostingRequestThread(eventQueue, successfulRequests, failedRequests, latch));
    }

    // Wait for event generation and request processing to complete
    try {
      latch.await(); // Wait for all requests to be processed
    } catch (InterruptedException e) {
      System.out.println("Main thread was interrupted: " + e.getMessage());
      Thread.currentThread().interrupt();
    }

    // Print results
    long totalTime = System.currentTimeMillis() - startTime;
    System.out.println("Thread counts: " + THREADS);
    System.out.println("Successful requests: " + successfulRequests.get());
    System.out.println("Failed requests: " + failedRequests.get());
    System.out.println("Total time: " + totalTime + " ms");
    System.out.println("Throughput: " + (TOTAL_REQUESTS / (totalTime / 1000.0)) + " requests/second");

    // Shutdown executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }
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
