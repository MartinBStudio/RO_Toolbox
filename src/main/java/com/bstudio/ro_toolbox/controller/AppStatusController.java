package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.RoToolboxApplication;
import com.bstudio.ro_toolbox.service.buffIcons.BuffIconsManagerService;
import com.bstudio.ro_toolbox.service.buffs.BuffsManagerService;
import com.bstudio.ro_toolbox.service.combatText.CombatTextManagerService;
import com.bstudio.ro_toolbox.service.lootModels.LootManagerService;
import com.bstudio.ro_toolbox.service.userInterface.UserInterfaceManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppStatusController {

    private final RoToolboxApplication app;
    private final LootManagerService lootManagerService;
    private final CombatTextManagerService combatTextManagerService;
    private final UserInterfaceManagerService userInterfaceManagerService;
    private final BuffIconsManagerService buffIconsManagerService;
    private final BuffsManagerService buffsManagerService;

    @GetMapping("/status")
    public AppStatusResponse status() {
        LootManagerService.ProfileInfo installed = lootManagerService.getInstalledProfileInfo();
        CombatTextManagerService.ProfileInfo installedCombatText = combatTextManagerService.getInstalledProfileInfo();
        UserInterfaceManagerService.ProfileInfo installedUserInterface = userInterfaceManagerService.getInstalledProfileInfo();
        BuffIconsManagerService.ProfileInfo installedBuffIcons = buffIconsManagerService.getInstalledProfileInfo();
        BuffsManagerService.ProfileInfo installedBuffs = buffsManagerService.getInstalledProfileInfo();
        return new AppStatusResponse(
                app.getVersion(),
                isTroseRunning(),
                new LootServiceSummaryResponse(
                        "/api/loot",
                        installed == null ? null : new ProfileInfoResponse(
                                installed.name,
                                installed.author,
                                installed.description,
                                installed.url,
                                installed.createdAt,
                                installed.version,
                                installed.managedSubfolders,
                                installed.disabledManagedSubfolders
                        )
                ),
                new CombatTextServiceSummaryResponse(
                        "/api/combattext",
                        installedCombatText == null ? null : new ProfileInfoResponse(
                                installedCombatText.name,
                                installedCombatText.author,
                                installedCombatText.description,
                                installedCombatText.url,
                                installedCombatText.createdAt,
                                installedCombatText.version,
                                List.of(),
                                List.of()
                        )
                ),
                new UserInterfaceServiceSummaryResponse(
                        "/api/userinterface",
                        installedUserInterface == null ? null : new ProfileInfoResponse(
                                installedUserInterface.name,
                                installedUserInterface.author,
                                installedUserInterface.description,
                                installedUserInterface.url,
                                installedUserInterface.createdAt,
                                installedUserInterface.version,
                                List.of(),
                                List.of()
                        )
                ),
                new BuffIconsServiceSummaryResponse(
                        "/api/bufficons",
                        installedBuffIcons == null ? null : new ProfileInfoResponse(
                                installedBuffIcons.name,
                                installedBuffIcons.author,
                                installedBuffIcons.description,
                                installedBuffIcons.url,
                                installedBuffIcons.createdAt,
                                installedBuffIcons.version,
                                List.of(),
                                List.of()
                        )
                ),
                new BuffsServiceSummaryResponse(
                        "/api/buffs",
                        installedBuffs == null ? null : new ProfileInfoResponse(
                                installedBuffs.name,
                                installedBuffs.author,
                                installedBuffs.description,
                                installedBuffs.url,
                                installedBuffs.createdAt,
                                installedBuffs.version,
                                List.of(),
                                List.of()
                        )
                ),
                List.of(
                        new ServiceEndpointResponse("lootService", "/api/loot", "Loot profiles and installation"),
                        new ServiceEndpointResponse("combatTextService", "/api/combattext", "Combat text profiles and installation"),
                        new ServiceEndpointResponse("userInterfaceService", "/api/userinterface", "User interface profiles and installation"),
                        new ServiceEndpointResponse("buffIconsService", "/api/bufficons", "Buff icons profiles and installation"),
                        new ServiceEndpointResponse("buffsService", "/api/buffs", "Buffs texture replacement profiles and installation"),
                        new ServiceEndpointResponse("configEditorService", "/api/config-editor", "ROSE config TOML editor"),
                        new ServiceEndpointResponse("settings", "/api/settings", "Generic app settings"),
                        new ServiceEndpointResponse("updater", "/api/update", "Backend updater checks and install")
                )
        );
    }

    public record AppStatusResponse(
            String version,
            boolean troseRunning,
            LootServiceSummaryResponse lootService,
            CombatTextServiceSummaryResponse combatTextService,
            UserInterfaceServiceSummaryResponse userInterfaceService,
            BuffIconsServiceSummaryResponse buffIconsService,
            BuffsServiceSummaryResponse buffsService,
            List<ServiceEndpointResponse> services
    ) {
    }

    static boolean isTroseRunning() {
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

    private static boolean isTroseRunningFromProcessHandles() {
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

    private static boolean isTroseRunningFromTasklist() {
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

    private static boolean isTroseRunningFromPowerShell() {
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

    private static String resolveTasklistPath() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            return "tasklist";
        }
        return systemRoot + "\\System32\\tasklist.exe";
    }

    static boolean tasklistOutputContainsTrose(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        String normalized = output.toLowerCase(Locale.ROOT);
        return normalized.contains("trose.exe");
    }

    private static boolean isTroseExecutable(String command) {
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

    public record LootServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record CombatTextServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record UserInterfaceServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record BuffIconsServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record BuffsServiceSummaryResponse(String endpoint, ProfileInfoResponse activeProfile) {
    }

    public record ServiceEndpointResponse(String key, String endpoint, String description) {
    }

    public record ProfileInfoResponse(String name, String author, String description, String url, String createdAt, String version, List<String> managedSubfolders, List<String> disabledManagedSubfolders) {
    }
}
