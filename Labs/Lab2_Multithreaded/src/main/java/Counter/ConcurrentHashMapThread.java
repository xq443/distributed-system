package Counter;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapThread implements Runnable {

  private ConcurrentHashMap<Integer, Integer> concurrentHashMap;
  private Integer num;

  // Constructor to initialize the map and the number of elements
  public ConcurrentHashMapThread(Integer num) {
    this.concurrentHashMap = new ConcurrentHashMap<>();
    this.num = num;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < num; i++) {
      concurrentHashMap.put(i, i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Time taken to add to ConcurrentHashMap: " + (endTime - startTime) + " ms");
  }
}