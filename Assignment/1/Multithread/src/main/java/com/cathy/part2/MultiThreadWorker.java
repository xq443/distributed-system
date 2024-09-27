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
  static final int TOTAL_REQUESTS = 200000;
  static final int INITIAL_THREADS = 64;
  static final int REQUESTS_PER_THREAD = 10;
  private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(TOTAL_REQUESTS * 2);
  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);
  private static PrintWriter fileWriter;
  private static final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>(); // for better concurrency without overhead lock

  public static void main(String[] args) throws IOException {
    // Setup the CSV file
    fileWriter = new PrintWriter(new FileWriter("test.csv"));
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

    CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

    // Start PostingRequestThreads
    int numTasks = (TOTAL_REQUESTS + REQUESTS_PER_THREAD - 1) / REQUESTS_PER_THREAD;
    for (int i = 0; i < numTasks; i++) {
      executor.execute(new PostingRequestThread());
    }

    // Wait for all threads to finish and shutdown executor
    try {
      latch.await();
    } catch (InterruptedException e) {
      System.err.println("Main thread interrupted: " + e.getMessage());
      Thread.currentThread().interrupt();
    }

    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Close the PrintWriter after all threads complete
    if (fileWriter != null) {
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

  // Add latencies safely
  public static void addLatency(long latency) {
    latencies.add(latency);
  }
}
