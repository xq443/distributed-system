package Counter;

public class CounterThread implements Runnable {

  @Override
  public void run() {
    // Increment the shared counter 10 times
    for (int i = 0; i < 10; i++) {
      Counter.incrementCounter();
    }
  }
}
