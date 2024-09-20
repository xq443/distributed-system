package Counter;

import java.util.Vector;

public class VectorThread implements Runnable{
  private Vector<Integer> vector;
  private Integer numElements;

  public VectorThread(Integer numElements) {
    this.vector = new Vector<>();
    this.numElements = numElements;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numElements; i++) {
      vector.add(i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Time taken to add to Vector: " + (endTime - startTime) + " ms");
  }

}