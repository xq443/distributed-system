package Counter;

import java.util.HashMap;
public class HashMapThread implements Runnable{
  private HashMap<Integer, Integer> map;
  private Integer num;

  public HashMapThread(Integer num) {
    this.map = new HashMap<>();
    this.num = num;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < num; i++) {
      map.put(i, i);
    }

    long endTime = System.currentTimeMillis();
    System.out.println("Time taken to add to HashMap: " + (endTime - startTime) + " ms");
  }
}
