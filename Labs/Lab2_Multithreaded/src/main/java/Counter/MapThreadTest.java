package Counter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MapThreadTest {
  // Number of elements to add
  private static final int NUM_ELEMENTS = 100_000;
  // Number of threads to use in multithreaded test
  private static final int NUM_THREADS = 100;

  public static void main(String[] args) throws InterruptedException {
    System.out.println("Single-threaded test:");
    // Single-threaded test
    testSingleThread();

    System.out.println("\nMultithreaded test with " + NUM_THREADS + " threads:");
    // Multithreaded test
    testMultiThread();
  }

  // Method for single-threaded performance test
  public static void testSingleThread() {
    // Test with HashTable
    Map<Integer, Integer> hashtable = new Hashtable<>();
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      hashtable.put(i, i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Time taken to add " + NUM_ELEMENTS + " elements to HashTable: " + (endTime - startTime) + " ms");

    // Test with HashMap
    Map<Integer, Integer> hashMap = new HashMap<>();
    startTime = System.currentTimeMillis();
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      hashMap.put(i, i);
    }
    endTime = System.currentTimeMillis();
    System.out.println("Time taken to add " + NUM_ELEMENTS + " elements to HashMap: " + (endTime - startTime) + " ms");

    // Test with ConcurrentHashMap
    Map<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<>();
    startTime = System.currentTimeMillis();
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      concurrentHashMap.put(i, i);
    }
    endTime = System.currentTimeMillis();
    System.out.println("Time taken to add " + NUM_ELEMENTS + " elements to ConcurrentHashMap: " + (endTime - startTime) + " ms");
  }

  // Method for multithreaded performance test
  public static void testMultiThread() throws InterruptedException {
    // Test with HashTable
    Map<Integer, Integer> hashtable = new Hashtable<>();
    long startTime = System.currentTimeMillis();
    runMultithreadedTest(hashtable);
    long endTime = System.currentTimeMillis();
    System.out.println("Time taken to add elements to HashTable (multithreaded): " + (endTime - startTime) + " ms");

    // Test with synchronized HashMap: thread-safe for hashmap
    Map<Integer, Integer> hashMap = Collections.synchronizedMap(new HashMap<>());
    startTime = System.currentTimeMillis();
    runMultithreadedTest(hashMap);
    endTime = System.currentTimeMillis();
    System.out.println("Time taken to add elements to synchronized HashMap (multithreaded): " + (endTime - startTime) + " ms");

    // Test with ConcurrentHashMap
    Map<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<>();
    startTime = System.currentTimeMillis();
    runMultithreadedTest(concurrentHashMap);
    endTime = System.currentTimeMillis();
    System.out.println("Time taken to add elements to ConcurrentHashMap (multithreaded): " + (endTime - startTime) + " ms");
  }

  // Helper method to run a multithreaded test on the provided map
  public static void runMultithreadedTest(Map<Integer, Integer> map) throws InterruptedException {
    Thread[] threads = new Thread[NUM_THREADS];

    // Initialize and start the threads
    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i] = new Thread(() -> {
        for (int j = 0; j < NUM_ELEMENTS / NUM_THREADS; j++) {
          map.put(j, j);
        }
      });
      threads[i].start();
    }

    // Wait for all threads to finish
    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i].join();
    }
  }
}
