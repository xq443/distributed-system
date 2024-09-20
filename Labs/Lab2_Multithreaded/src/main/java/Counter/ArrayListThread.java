package Counter;

public class ArrayListThread implements Runnable{
  private java.util.ArrayList array;
  private Integer numElements;

  public ArrayListThread(Integer numElements) {
    this.array = new java.util.ArrayList<>();
    this.numElements = numElements;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numElements; i++) {
      array.add(i);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Time taken to add to ArrayList: " + (endTime - startTime) + " ms");
  }
}
