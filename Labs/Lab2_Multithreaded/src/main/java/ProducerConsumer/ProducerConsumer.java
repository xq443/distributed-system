package ProducerConsumer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ProducerConsumer {

  /**
   * Time taken (approach with thread pool): 560 ms
   * Time taken (approach with thread pool after sorting): 1333 ms
   * Reduce thread creation overhead
   * Reuse of threads
   */
  private static final int NUMBER_OF_THREADS = 500;
  private static final int CAPACITY = 1000;
  private static final String FILE_NAME = "BlockingQueue.txt";

  private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

  /**
   * Blocking Queue
   * Concurrent Access: BlockingQueue is designed to be thread-safe,
   * multiple threads can safely interact with it without the need for additional synchronization.
   */


  public static void main(String[] args) throws InterruptedException {
    long startTime = System.currentTimeMillis();

    // Start single file-writing thread
    Thread writerThread = new Thread(() -> {
      List<String> collectedData = new ArrayList<>();
      try {
        while (true) {
          String data = queue.take();
          if (data.equals("END")) break;
          collectedData.add(data);
        }

        // Sort the collected data by timestamp
        Collections.sort(collectedData, Comparator.comparingLong(a -> Long.parseLong(a.split(",")[0])));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
          for(String data : collectedData) {
            writer.write(data);
          }
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });
    writerThread.start();

    // Create a fixed thread pool
    ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Submit multi data-generating tasks to the thread pool
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      executor.submit(new Writer4Task(queue, CAPACITY));
    }

    // Shut down the executor and wait for tasks to complete
    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

    // Signal the writer thread to stop
    queue.put("END");

    // Wait for the writer thread to complete
    writerThread.join();

    long endTime = System.currentTimeMillis();
    System.out.println("Time taken (approach with thread pool): " + (endTime - startTime) + " ms");
  }
}
