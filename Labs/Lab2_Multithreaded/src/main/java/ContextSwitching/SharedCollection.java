package ContextSwitching;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedCollection extends Thread {
  private final int capacity;

  // thread-safe, non-blocking queue that allows multiple threads to add data concurrently
  // without explicit locks.
  private final List<String> sharedQueue;


  public SharedCollection(int capacity) {
    this.capacity = capacity;
    this.sharedQueue = new ArrayList<>();
  }

  @Override
  public void run() {
    for (int i = 0; i < capacity; i++) {
      String data = System.currentTimeMillis() + ", " + this.getId() + ", " + i + "\n";
      sharedQueue.add(data);
    }
  }

  public List<String> getLocalData() {
    return sharedQueue;
  }
}

