package com.bstudio.ro_toolbox.service.loginManager;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

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
    void createsUpdatesAndDeletesAccounts() throws IOException {
        LoginManagerService service = new LoginManagerService();
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

            LoginManagerService.LoginAccount updated = service.updateAccount(first.id(), new LoginManagerService.UpdateAccountRequest(
                    "Alpha Updated",
                    "alpha+updated@example.com",
                    "new-pass",
                    Boolean.FALSE,
                    "🧑‍💻"
            ));
            assertEquals("Alpha Updated", updated.name());
            assertFalse(updated.displayInQuick());

            LoginManagerService.LoginAccount deleted = service.deleteAccount(second.id());
            assertEquals("Beta", deleted.name());
            assertEquals(1, service.listAccounts().size());
        } finally {
            Files.deleteIfExists(service.getAccountsFile());
        }
    }
}
