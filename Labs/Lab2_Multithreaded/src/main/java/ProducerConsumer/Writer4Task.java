package ProducerConsumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class Writer4Task implements Runnable {
  //private final BlockingQueue<String> queue;
  private final BlockingQueue<String> queue;
  private final int CAPACITY;

  public Writer4Task(BlockingQueue<String> queue, int CAPACITY) {
    this.queue = queue;
    this.CAPACITY = CAPACITY;
  }

  @Override
  public void run() {
    for (int i = 0; i < CAPACITY; i++) {
      // Generate data including current time, thread ID, and counter
      long currentTime = System.currentTimeMillis();
      long threadId = Thread.currentThread().getId();
      String data = currentTime + ", " + threadId + ", " + i + "\n";
      try {
        queue.put(data);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
