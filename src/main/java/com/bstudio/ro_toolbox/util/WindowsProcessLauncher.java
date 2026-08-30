package com.bstudio.ro_toolbox.util;

import java.io.IOException;
import java.nio.file.Path;

public final class WindowsProcessLauncher {
    private WindowsProcessLauncher() {
    }

    public static void launchForeground(Path workingDirectory, String executablePath, String... arguments) throws IOException {
        StringBuilder argsExpression = new StringBuilder("@(");
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    argsExpression.append(",");
                }
                argsExpression.append("'").append(psQuote(arguments[i])).append("'");
            }
        }
        argsExpression.append(")");

        String script =
                "$p=Start-Process -FilePath '" + psQuote(executablePath) + "'" +
                        " -WorkingDirectory '" + psQuote(workingDirectory.toAbsolutePath().toString()) + "'" +
                        " -ArgumentList " + argsExpression +
                        " -WindowStyle Normal -PassThru;" +
                        "Start-Sleep -Milliseconds 260;" +
                        "try{(New-Object -ComObject WScript.Shell).AppActivate($p.Id)|Out-Null}catch{}";

        new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script).start();
    }

    public static void openFolderForeground(Path folderPath) throws IOException {
        String script =
                "$p=Start-Process -FilePath explorer.exe -ArgumentList '" + psQuote(folderPath.toAbsolutePath().toString()) + "'" +
                        " -WindowStyle Normal -PassThru;" +
                        "Start-Sleep -Milliseconds 160;" +
                        "try{(New-Object -ComObject WScript.Shell).AppActivate($p.Id)|Out-Null}catch{}";
        new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script).start();
    }

    private static String psQuote(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
