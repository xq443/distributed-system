package ContextSwitching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Batching extends Thread {
  private final int capacity;

  public Batching(int capacity) {
    this.capacity = capacity;
  }

  @Override
  public void run() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < capacity; i++) {
      String data = System.currentTimeMillis() + ", " + this.getId() + ", " + i + "\n";
      stringBuilder.append(data);
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter("Batching.text", true))) {
      writer.write(stringBuilder.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
