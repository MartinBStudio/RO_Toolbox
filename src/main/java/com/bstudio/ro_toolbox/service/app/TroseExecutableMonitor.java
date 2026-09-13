package com.bstudio.ro_toolbox.service.app;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

@Service
public class TroseExecutableMonitor {

    public boolean isTroseRunning() {
        if (isTroseRunningFromProcessHandles()) {
            return true;
        }
        if (!isWindows()) {
            return false;
        }
        if (isTroseRunningFromTasklist()) {
            return true;
        }
        return isTroseRunningFromPowerShell();
    }

    private boolean isTroseRunningFromProcessHandles() {
        try {
            return ProcessHandle.allProcesses()
                    .map(ProcessHandle::info)
                    .anyMatch(info -> isTroseExecutable(info.command().orElse(null))
                            || isTroseExecutable(info.commandLine().orElse(null)));
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private boolean isTroseRunningFromTasklist() {
        String tasklistPath = resolveTasklistPath();
        try {
            Process process = new ProcessBuilder(tasklistPath, "/FI", "IMAGENAME eq trose.exe", "/FO", "CSV", "/NH")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            process.waitFor();
            return tasklistOutputContainsTrose(output);
        } catch (IOException ignored) {
            return false;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isTroseRunningFromPowerShell() {
        try {
            Process process = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    "(Get-Process -Name trose -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty ProcessName)"
            ).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            process.waitFor();
            return output.toLowerCase(Locale.ROOT).contains("trose");
        } catch (IOException ignored) {
            return false;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String resolveTasklistPath() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            return "tasklist";
        }
        return systemRoot + "\\System32\\tasklist.exe";
    }

    public boolean tasklistOutputContainsTrose(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        String normalized = output.toLowerCase(Locale.ROOT);
        return normalized.contains("trose.exe");
    }

    private boolean isTroseExecutable(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        String value = command.trim().replace("\"", "").toLowerCase(Locale.ROOT);
        int executableEnd = value.indexOf(".exe");
        if (executableEnd >= 0) {
            value = value.substring(0, executableEnd + 4);
        }
        int separatorIndex = Math.max(value.lastIndexOf('\\'), value.lastIndexOf('/'));
        String fileName = separatorIndex >= 0 ? value.substring(separatorIndex + 1) : value;
        return "trose.exe".equals(fileName);
    }
}
