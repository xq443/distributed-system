package Counter;

import java.util.Hashtable;

public class HashTableThread implements Runnable {
  private Hashtable<Integer, Integer> hashTable;
  private Integer num;

  public HashTableThread(Integer num) {
    this.hashTable = new Hashtable<>();
    this.num = num;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < num; i++) {
      hashTable.put(i, i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Time taken to add to HashTable: " + (endTime - startTime) + " ms");

  }
}
