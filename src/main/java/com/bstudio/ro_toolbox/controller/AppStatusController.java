package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.service.app.AppNotificationService;
import com.bstudio.ro_toolbox.service.app.TroseExecutableMonitor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppStatusController {

  private final RoToolboxApplication app;
  private final AppNotificationService appNotificationService;
  private final TroseExecutableMonitor troseExecutableMonitor;

  @GetMapping("/status")
  public AppStatusResponse status() {
    return new AppStatusResponse(app.getVersion(), troseExecutableMonitor.isTroseRunning());
  }

  @GetMapping("/notifications/drain")
  public NotificationQueueResponse drainNotifications() {
    return new NotificationQueueResponse(appNotificationService.drain());
  }

  public record AppStatusResponse(String version, boolean troseRunning) {}

  public record NotificationQueueResponse(List<String> messages) {}
}
