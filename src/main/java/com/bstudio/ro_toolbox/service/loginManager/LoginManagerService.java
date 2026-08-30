package com.bstudio.ro_toolbox.service.loginManager;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class LoginManagerService {
    private static final String ENCRYPTION_PREFIX = "enc:";
    private static final String ENCRYPTION_SECRET = "RO_Toolbox::login-vault::v1::" + System.getProperty("user.home", "") + "::" + System.getProperty("user.name", "");
    private final Path configDir;
    private final Path accountsFile;

    public LoginManagerService() {
        this(resolveAppDataRoot());
    }

    LoginManagerService(Path appDataRoot) {
        Path root = appDataRoot == null ? resolveAppDataRoot() : appDataRoot;
        this.configDir = root.resolve("config");
        this.accountsFile = this.configDir.resolve("accounts.properties");
    }

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
        return accountsFile;
    }

    private List<LoginAccount> readAccounts() throws IOException {
        Files.createDirectories(configDir);
        if (!Files.exists(accountsFile) || Files.size(accountsFile) == 0) {
            return new ArrayList<>();
        }

        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(accountsFile)) {
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
            String resolvedPassword = decryptIfNeeded(password);
            accounts.add(new LoginAccount(
                    id,
                    name,
                    email,
                    resolvedPassword,
                    displayValue == null ? Boolean.TRUE : Boolean.parseBoolean(displayValue),
                    iconValue == null || iconValue.isBlank() ? "👤" : iconValue
            ));
        }
        return accounts;
    }

    private void writeAccounts(List<LoginAccount> accounts) throws IOException {
        Files.createDirectories(configDir);
        Properties props = new Properties();
        for (LoginAccount account : accounts) {
            String prefix = "account." + account.id() + ".";
            props.setProperty(prefix + "name", account.name());
            props.setProperty(prefix + "email", account.email());
            props.setProperty(prefix + "password", encryptIfNeeded(account.password()));
            props.setProperty(prefix + "displayInQuick", String.valueOf(Boolean.TRUE.equals(account.displayInQuick())));
            props.setProperty(prefix + "icon", account.icon());
        }

        try (OutputStream output = Files.newOutputStream(accountsFile)) {
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

    private String encryptIfNeeded(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.startsWith(ENCRYPTION_PREFIX)) {
            return value;
        }
        try {
            return ENCRYPTION_PREFIX + encrypt(value);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt password for local storage.", e);
        }
    }

    private String decryptIfNeeded(String value) {
        if (value == null) {
            return null;
        }
        if (!value.startsWith(ENCRYPTION_PREFIX)) {
            return value;
        }
        try {
            return decrypt(value.substring(ENCRYPTION_PREFIX.length()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to decrypt saved password.", e);
        }
    }

    private String encrypt(String rawValue) throws GeneralSecurityException {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] keyBytes = deriveKey();
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String encodedValue) throws GeneralSecurityException {
        byte[] combined = Base64.getDecoder().decode(encodedValue);
        if (combined.length <= 12) {
            throw new IllegalStateException("Encrypted payload is invalid.");
        }
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[combined.length - 12];
        System.arraycopy(combined, 0, iv, 0, 12);
        System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(deriveKey(), "AES"), new GCMParameterSpec(128, iv));
        byte[] plaintext = cipher.doFinal(encrypted);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private byte[] deriveKey() throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] material = digest.digest(ENCRYPTION_SECRET.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(material, 0, key, 0, key.length);
        return key;
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
