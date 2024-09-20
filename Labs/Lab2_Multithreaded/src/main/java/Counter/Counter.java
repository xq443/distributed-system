package Counter;

public class Counter {
  // Shared counter
  private static int counter = 0;

  public static synchronized void incrementCounter() {
    counter++;
  }

  // Synchronized accessor for the counter
  public static synchronized int getCounter() {
    return counter;
  }

  public static void main(String[] args) throws InterruptedException {

    //default is 1K
    int numThreads = (args.length > 0) ? Integer.parseInt(args[0]) : 1000;

    // Take a timestamp before starting the threads
    long startTime = System.currentTimeMillis();

    // Array to hold all the threads
    Thread[] threads = new Thread[numThreads];

    // Initialize and start the threads
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(new CounterThread());
      threads[i].start();
    }

    // the main thread waits for the i-th thread in the array to finish before moving on to the next thread.
    for (int i = 0; i < numThreads; i++) {
      threads[i].join();
    }

    // Take a timestamp after all threads have completed
    long endTime = System.currentTimeMillis();

    System.out.println("Counter.Counter value: " + getCounter());
    System.out.println("Duration (ms): " + (endTime - startTime));
  }
}