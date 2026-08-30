package com.bstudio.ro_toolbox.service.loginManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LoginManagerServiceTests {

    @Test
    void defaultsDisplayInQuickToTrueAndUsesDefaultIcon() {
        LoginManagerService.LoginAccount account = new LoginManagerService.LoginAccount(
                "account-id",
                "Test User",
                "user@example.com",
                "secret",
                null,
                null
        );

        assertTrue(account.displayInQuick());
        assertEquals("👤", account.icon());
    }

    @Test
    void createsUpdatesAndDeletesAccounts(@TempDir Path tempDir) throws IOException {
        LoginManagerService service = new LoginManagerService(tempDir);
        Files.deleteIfExists(service.getAccountsFile());
        try {
            LoginManagerService.LoginAccount first = service.createAccount(new LoginManagerService.CreateAccountRequest(
                    "Alpha",
                    "alpha@example.com",
                    "pass1",
                    null,
                    "🧑"
            ));
            LoginManagerService.LoginAccount second = service.createAccount(new LoginManagerService.CreateAccountRequest(
                    "Beta",
                    "beta@example.com",
                    "pass2",
                    Boolean.FALSE,
                    "🤖"
            ));

            assertEquals(2, service.listAccounts().size());
            assertTrue(service.listQuickAccounts().stream().anyMatch(account -> account.id().equals(first.id())));
            assertFalse(service.listQuickAccounts().stream().anyMatch(account -> account.id().equals(second.id())));
            assertTrue(Files.readString(service.getAccountsFile()).contains("enc"));
            assertEquals("pass1", service.listAccounts().stream().filter(account -> account.id().equals(first.id())).findFirst().orElseThrow().password());

            LoginManagerService.LoginAccount updated = service.updateAccount(first.id(), new LoginManagerService.UpdateAccountRequest(
                    "Alpha Updated",
                    "alpha+updated@example.com",
                    "new-pass",
                    Boolean.FALSE,
                    "🧑‍💻"
            ));
            assertEquals("Alpha Updated", updated.name());
            assertFalse(updated.displayInQuick());
            assertEquals("new-pass", service.listAccounts().stream().filter(account -> account.id().equals(first.id())).findFirst().orElseThrow().password());

            LoginManagerService.LoginAccount deleted = service.deleteAccount(second.id());
            assertEquals("Beta", deleted.name());
            assertEquals(1, service.listAccounts().size());

            LoginManagerService.ExportAccountsResponse exported = service.exportAccounts();
            assertEquals(1, exported.version());
            assertEquals(1, exported.accounts().size());
            String exportedPassword = exported.accounts().get(0).password();
            assertTrue(exportedPassword.startsWith("enc:"));
            assertNotEquals("new-pass", exportedPassword);

            service.importAccounts(new LoginManagerService.ImportAccountsRequest(
                    java.util.List.of(
                            new LoginManagerService.ImportLoginAccount(
                                    null,
                                    "Gamma",
                                    "gamma@example.com",
                                    "pass3",
                                    Boolean.TRUE,
                                    "⚔️"
                            )
                    ),
                    Boolean.TRUE
            ));
            assertEquals(1, service.listAccounts().size());
            assertEquals("Gamma", service.listAccounts().get(0).name());
        } finally {
            Files.deleteIfExists(service.getAccountsFile());
        }
    }
}
