package com.bstudio.ro_toolbox.controller.resourceReplacer;

import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.utils.ICommonResourceMethods;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public abstract class BaseResourceReplacerController implements ICommonResourceMethods {
  @FunctionalInterface
  protected interface ProgressUpdateRunner {
    void run(java.util.function.Consumer<ResourcesUpdater.DownloadProgress> progressListener)
        throws IOException;
  }

  protected StreamingResponseBody downloadWithProgress(
      ProgressUpdateRunner runner, String downloadingMessage, String completeMessage) {
    return outputStream -> {
      try {
        writeProgressEvent(outputStream, "started", 0L, -1L, downloadingMessage);
        runner.run(
            progress ->
                writeProgressEvent(
                    outputStream,
                    "progress",
                    progress.downloadedBytes(),
                    progress.totalBytes(),
                    null));
        writeProgressEvent(outputStream, "complete", 0L, 0L, completeMessage);
      } catch (Exception ex) {
        writeProgressEvent(
            outputStream,
            "error",
            0L,
            0L,
            ex.getMessage() == null ? "Resource update failed." : ex.getMessage());
      }
    };
  }

  private void writeProgressEvent(
      OutputStream outputStream,
      String type,
      long downloadedBytes,
      long totalBytes,
      String message) {
    try {
      StringBuilder json = new StringBuilder();
      json.append("{\"type\":\"").append(escapeJson(type)).append("\"");
      json.append(",\"downloadedBytes\":").append(downloadedBytes);
      json.append(",\"totalBytes\":").append(totalBytes);
      if (message != null) {
        json.append(",\"message\":\"").append(escapeJson(message)).append("\"");
      }
      json.append("}\n");
      outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
