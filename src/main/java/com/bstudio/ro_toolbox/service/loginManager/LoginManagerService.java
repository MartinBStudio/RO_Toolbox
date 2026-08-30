package com.bstudio.ro_toolbox.service.loginManager;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class LoginManagerService {
    private static final Path APP_DATA_ROOT = resolveAppDataRoot();
    private static final Path CONFIG_DIR = APP_DATA_ROOT.resolve("config");
    private static final Path ACCOUNTS_FILE = CONFIG_DIR.resolve("accounts.properties");

    public List<LoginAccount> listAccounts() throws IOException {
        return readAccounts();
    }

    public List<LoginAccount> listQuickAccounts() throws IOException {
        return readAccounts().stream()
                .filter(account -> Boolean.TRUE.equals(account.displayInQuick()))
                .toList();
    }

    public LoginAccount createAccount(CreateAccountRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("Account payload is required.");
        }

        LoginAccount account = new LoginAccount(
                UUID.randomUUID().toString(),
                normalizeText(request.name(), "name"),
                normalizeText(request.email(), "email"),
                normalizeText(request.password(), "password"),
                request.displayInQuick() == null ? Boolean.TRUE : request.displayInQuick(),
                normalizeIcon(request.icon())
        );

        List<LoginAccount> accounts = readAccounts();
        accounts.add(account);
        writeAccounts(accounts);
        return account;
    }

    public LoginAccount updateAccount(String id, UpdateAccountRequest request) throws IOException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Account id is required.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Account payload is required.");
        }

        List<LoginAccount> accounts = readAccounts();
        LoginAccount existing = accounts.stream()
                .filter(entry -> id.equals(entry.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        LoginAccount updated = new LoginAccount(
                existing.id(),
                normalizeText(request.name(), "name"),
                normalizeText(request.email(), "email"),
                normalizeText(request.password(), "password"),
                request.displayInQuick() == null ? existing.displayInQuick() : request.displayInQuick(),
                normalizeIcon(request.icon())
        );

        List<LoginAccount> replaced = new ArrayList<>();
        for (LoginAccount account : accounts) {
            if (id.equals(account.id())) {
                replaced.add(updated);
            } else {
                replaced.add(account);
            }
        }
        writeAccounts(replaced);
        return updated;
    }

    public LoginAccount deleteAccount(String id) throws IOException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Account id is required.");
        }

        List<LoginAccount> accounts = readAccounts();
        LoginAccount removed = accounts.stream()
                .filter(entry -> id.equals(entry.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        List<LoginAccount> filtered = accounts.stream()
                .filter(entry -> !id.equals(entry.id()))
                .toList();
        writeAccounts(filtered);
        return removed;
    }

    public Path getAccountsFile() {
        return ACCOUNTS_FILE;
    }

    private List<LoginAccount> readAccounts() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        if (!Files.exists(ACCOUNTS_FILE) || Files.size(ACCOUNTS_FILE) == 0) {
            return new ArrayList<>();
        }

        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(ACCOUNTS_FILE)) {
            props.load(input);
        }

        Map<String, Map<String, String>> grouped = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("account.")) {
                continue;
            }
            int secondDot = key.indexOf('.', "account.".length());
            if (secondDot < 0) {
                continue;
            }
            String id = key.substring("account.".length(), secondDot);
            String field = key.substring(secondDot + 1);
            grouped.computeIfAbsent(id, ignored -> new LinkedHashMap<>()).put(field, props.getProperty(key));
        }

        List<LoginAccount> accounts = new ArrayList<>();
        for (String id : new TreeSet<>(grouped.keySet())) {
            Map<String, String> data = grouped.get(id);
            String name = data.get("name");
            String email = data.get("email");
            String password = data.get("password");
            if (name == null || email == null || password == null) {
                continue;
            }
            String displayValue = data.get("displayInQuick");
            String iconValue = data.get("icon");
            accounts.add(new LoginAccount(
                    id,
                    name,
                    email,
                    password,
                    displayValue == null ? Boolean.TRUE : Boolean.parseBoolean(displayValue),
                    iconValue == null || iconValue.isBlank() ? "👤" : iconValue
            ));
        }
        return accounts;
    }

    private void writeAccounts(List<LoginAccount> accounts) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Properties props = new Properties();
        for (LoginAccount account : accounts) {
            String prefix = "account." + account.id() + ".";
            props.setProperty(prefix + "name", account.name());
            props.setProperty(prefix + "email", account.email());
            props.setProperty(prefix + "password", account.password());
            props.setProperty(prefix + "displayInQuick", String.valueOf(Boolean.TRUE.equals(account.displayInQuick())));
            props.setProperty(prefix + "icon", account.icon());
        }

        try (OutputStream output = Files.newOutputStream(ACCOUNTS_FILE)) {
            props.store(output, "RO_Toolbox login accounts");
        }
    }

    private String normalizeText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String normalizeIcon(String icon) {
        if (icon == null || icon.isBlank()) {
            return "👤";
        }
        return icon.trim();
    }

    private static Path resolveAppDataRoot() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "RO_Toolbox");
        }
        return Path.of(System.getProperty("user.home"), ".ro_toolbox");
    }

    public record LoginAccount(
            String id,
            String name,
            String email,
            String password,
            Boolean displayInQuick,
            String icon
    ) {
        public LoginAccount {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(name, "name is required");
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(password, "password is required");
            displayInQuick = displayInQuick == null ? Boolean.TRUE : displayInQuick;
            icon = (icon == null || icon.isBlank()) ? "👤" : icon.trim();
        }
    }

    public record CreateAccountRequest(String name, String email, String password, Boolean displayInQuick, String icon) {
    }

    public record UpdateAccountRequest(String name, String email, String password, Boolean displayInQuick, String icon) {
    }
}
