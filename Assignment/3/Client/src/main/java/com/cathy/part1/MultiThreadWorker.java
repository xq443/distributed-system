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
  public static final int INITIAL_THREADS = 32; // Start with 32 threads
  public static final int PHASE_TWO_THREADS = 290;
  public static final int REQUESTS_PER_THREAD = 1000; // Each thread sends 1000 requests
  private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(TOTAL_REQUESTS * 5);
  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);
  private static final CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS); // Latch for total requests
  private static volatile boolean phaseOneComplete = false; // Track if initial phase is complete

  public static void main(String[] args) {
    // Start request generation
    Thread eventGeneratorThread = new Thread(new EventGenerator());
    eventGeneratorThread.start();

    long startTime = System.currentTimeMillis();

    // Create ThreadPoolExecutor with initial thread pool size
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        INITIAL_THREADS,
        PHASE_TWO_THREADS, // Max pool size for phase two
        5000L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // Start initial posting threads
    for (int i = 0; i < INITIAL_THREADS; i++) {
      executor.execute(new PostingRequestThread(latch, executor));
    }

    // Wait for event generation to complete
    try {
      latch.await(); // Wait for all requests to be processed
    } catch (InterruptedException e) {
      System.out.println("Main thread was interrupted: " + e.getMessage());
      Thread.currentThread().interrupt();
    }

    // Print results
    long totalTime = System.currentTimeMillis() - startTime;
    System.out.println("Thread counts: " + PHASE_TWO_THREADS);
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

  public static CountDownLatch getLatch() {
    return latch;
  }

  public static void markPhaseOneComplete() {
    phaseOneComplete = true; // Mark phase one as complete
  }

  public static boolean isPhaseOneComplete() {
    return phaseOneComplete;
  }
}
