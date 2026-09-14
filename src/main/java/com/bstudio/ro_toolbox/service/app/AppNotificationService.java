package com.bstudio.ro_toolbox.service.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Service;

@Service
public class AppNotificationService {

  private final Queue<String> queuedMessages = new ConcurrentLinkedQueue<>();

  public void enqueue(String message) {
    if (message == null || message.isBlank()) {
      return;
    }
    queuedMessages.offer(message.trim());
  }

  public List<String> drain() {
    List<String> messages = new ArrayList<>();
    String current;
    while ((current = queuedMessages.poll()) != null) {
      messages.add(current);
    }
    return messages;
  }
}
