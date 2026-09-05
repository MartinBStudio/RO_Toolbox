package com.bstudio.ro_toolbox.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppStatusControllerTests {

    @Test
    void detectsTroseInTasklistCsvOutput() {
        String output = "\"trose.exe\",\"1234\",\"Console\",\"1\",\"123,456 K\"";
        assertTrue(AppStatusController.tasklistOutputContainsTrose(output));
    }

    @Test
    void detectsTroseInTasklistTableOutput() {
        String output = "trose.exe                    1234 Console                    1    123,456 K";
        assertTrue(AppStatusController.tasklistOutputContainsTrose(output));
    }

    @Test
    void ignoresNoTasksTasklistOutput() {
        String output = "INFO: No tasks are running which match the specified criteria.";
        assertFalse(AppStatusController.tasklistOutputContainsTrose(output));
    }
}
