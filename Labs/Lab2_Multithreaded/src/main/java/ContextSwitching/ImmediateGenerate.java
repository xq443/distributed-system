package ContextSwitching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ImmediateGenerate extends Thread {
  private final int capacity;

  public ImmediateGenerate(int capacity) {
    this.capacity = capacity;
  }

  @Override
  public void run() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("ImmediateGenerated.txt", true))) {
      for (int i = 0; i < capacity; i++) {
        String data = System.currentTimeMillis() + ", " + this.getId() + ", " + i + "\n";
          writer.write(data);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
