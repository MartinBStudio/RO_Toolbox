package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.updater.UpdaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/update")
@RequiredArgsConstructor
public class UpdateController {

    private final UpdaterService updaterService;

    @GetMapping("/check")
    public UpdaterService.UpdateCheckResult checkForUpdate() {
        return updaterService.checkForUpdate();
    }

    @GetMapping("/latest-release/download")
    public ResponseEntity<StreamingResponseBody> downloadLatestRelease() throws java.io.IOException {
        UpdaterService.ReleaseDownload download = updaterService.openLatestReleaseDownload();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(download.fileName()).build());
        if (download.contentLength() >= 0) {
            headers.setContentLength(download.contentLength());
        }

        StreamingResponseBody body = outputStream -> {
            try (download) {
                download.inputStream().transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    @PostMapping("/install")
    public UpdaterService.UpdateInstallResult installUpdate() {
        return updaterService.installUpdate(updaterService.checkForUpdate());
    }
}
