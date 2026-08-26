package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.updater.UpdaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/update")
@RequiredArgsConstructor
public class UpdateController {

    private final UpdaterService updaterService;

    @GetMapping("/check")
    public UpdaterService.UpdateCheckResult checkForUpdate() {
        return updaterService.checkForUpdate();
    }

    @PostMapping("/install")
    public UpdaterService.UpdateInstallResult installUpdate() {
        return updaterService.installUpdate(updaterService.checkForUpdate());
    }
}
