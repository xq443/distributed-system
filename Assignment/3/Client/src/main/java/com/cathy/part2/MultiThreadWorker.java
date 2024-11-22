package com.cathy.part2;

import com.cathy.bean.LiftRideEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadWorker {
  public static final int TOTAL_REQUESTS = 200000;
  public static final int INITIAL_THREADS = 190;
  public static final int REQUESTS_PER_THREAD = 10;
  private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(TOTAL_REQUESTS * 4);
  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);
  private static final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>(); // for better concurrency without overhead lock

  public static void main(String[] args) throws IOException {
    // Set up the CSV file
    PrintWriter fileWriter = new PrintWriter(new FileWriter("test.csv"));
    fileWriter.println("StartTime,RequestType,Latency,ResponseCode");

    // Start event generation in a separate thread
    Thread eventGeneratorThread = new Thread(new EventGenerator());
    eventGeneratorThread.start();

    long startTime = System.currentTimeMillis();

    // Create ThreadPoolExecutor
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        INITIAL_THREADS,
        INITIAL_THREADS,
        5000L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS); // Change TOTAL_REQUESTS to INITIAL_THREADS

    // Start PostingRequestThreads
    int numTasks = (TOTAL_REQUESTS + REQUESTS_PER_THREAD - 1) / REQUESTS_PER_THREAD;
    for (int i = 0; i < numTasks; i++) {
      executor.execute(new PostingRequestThread(latch, latencies, fileWriter)); // Pass latch to the PostingRequestThread
    }

    // Wait for all threads to finish and shutdown executor
    try {
      latch.await(); // Wait for all threads to complete
    } catch (InterruptedException e) {
      System.err.println("Main thread interrupted: " + e.getMessage());
      Thread.currentThread().interrupt();
    } finally {
      // Close the PrintWriter after all threads complete
      fileWriter.flush();
      fileWriter.close();
    }

    // Print results and performance metrics
    long totalTime = System.currentTimeMillis() - startTime;
    System.out.println("Successful requests: " + successfulRequests.get());
    System.out.println("Failed requests: " + failedRequests.get());
    System.out.println("Total time: " + totalTime + " ms");
    System.out.println("Throughput: " + (TOTAL_REQUESTS / (totalTime / 1000.0)) + " requests/second");
    generatePerformanceMetrics();
    executor.shutdown();
  }

  private static void generatePerformanceMetrics() {
    if (latencies.isEmpty()) return;

    List<Long> sortedLatencies = new ArrayList<>(latencies);
    sortedLatencies.sort(Long::compareTo);

    int size = sortedLatencies.size();
    long totalLatencies = sortedLatencies.stream().mapToLong(Long::longValue).sum();
    long mean = totalLatencies / size;
    long median = sortedLatencies.get(size / 2);
    long p99 = sortedLatencies.get((int) (size * 0.99));
    long min = sortedLatencies.get(0);
    long max = sortedLatencies.get(size - 1);

    System.out.println("Mean response time: " + mean + " ms");
    System.out.println("Median response time: " + median + " ms");
    System.out.println("p99 response time: " + p99 + " ms");
    System.out.println("Min response time: " + min + " ms");
    System.out.println("Max response time: " + max + " ms");
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
