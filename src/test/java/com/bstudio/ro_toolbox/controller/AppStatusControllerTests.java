package com.bstudio.ro_toolbox.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bstudio.ro_toolbox.service.app.TroseExecutableMonitor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppStatusControllerTests {
  @Autowired private TroseExecutableMonitor troseExecutableMonitor;

  @Test
  void detectsTroseInTasklistCsvOutput() {
    String output = "\"trose.exe\",\"1234\",\"Console\",\"1\",\"123,456 K\"";
    assertTrue(troseExecutableMonitor.tasklistOutputContainsTrose(output));
  }

  @Test
  void detectsTroseInTasklistTableOutput() {
    String output = "trose.exe                    1234 Console                    1    123,456 K";
    assertTrue(troseExecutableMonitor.tasklistOutputContainsTrose(output));
  }

  @Test
  void ignoresNoTasksTasklistOutput() {
    String output = "INFO: No tasks are running which match the specified criteria.";
    assertFalse(troseExecutableMonitor.tasklistOutputContainsTrose(output));
  }
}
