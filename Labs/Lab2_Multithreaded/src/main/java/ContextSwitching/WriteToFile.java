package ContextSwitching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WriteToFile {

  /**
   * Time taken (approach 1): 884 ms
   * Time taken (approach 2): 1356 ms
   * Time taken (approach 3): 1942 ms
   * @param args
   * @throws InterruptedException
   */

  public static void main(String[] args) throws InterruptedException {

    int capacity = 5000;

    WriteToFile writerToFile = new WriteToFile();

    writerToFile.ImmediateGenerate(capacity);

    writerToFile.Batching(capacity);

    writerToFile.SharedCollection(capacity);

  }

  public void ImmediateGenerate(int capacity) throws InterruptedException {
    long startTime = System.currentTimeMillis();
    Thread[] threads = new Thread[500];

    for (int i = 0; i < threads.length; i++) {
      threads[i] = new ImmediateGenerate(capacity);
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    long endTime = System.currentTimeMillis();
    System.out.println("Time taken (approach 1): " + (endTime - startTime) + " ms");
  }

  public void Batching(int capacity) throws InterruptedException {
    long startTime = System.currentTimeMillis();
    Thread[] threads = new Thread[500];

    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Batching(capacity);
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    long endTime = System.currentTimeMillis();
    System.out.println("Time taken (approach 2): " + (endTime - startTime) + " ms");
  }

  public void SharedCollection(int capacity) throws InterruptedException {
    StringBuilder fileBuilder = new StringBuilder();
    List<SharedCollection> threadList = new ArrayList<>();
    long startTime = System.currentTimeMillis();
    Thread[] threads = new Thread[500];


    for (int i = 0; i < threads.length; i++) {
      SharedCollection thread = new SharedCollection(capacity);
      threads[i] = thread;
      threadList.add(thread);
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (SharedCollection thread : threadList) {
      for (String data : thread.getLocalData()) {
        fileBuilder.append(data);
      }
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter("SharedCollection.txt", true))) {
      writer.write(fileBuilder.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }

    long endTime = System.currentTimeMillis();
    System.out.println("Time taken (approach 3): " + (endTime - startTime) + " ms");
  }
}