package com.pnc.masters.document;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@ConfigurationProperties(prefix = "document.storage.local")
public class LocalFolderDocumentStorage implements DocumentStorage {

    private Path root = Path.of("./images/customer-logos");

    public void setRoot(Path root) { this.root = root; }

    @Override
    public StoredDocument store(String storageKey, InputStream content) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            MessageDigest digest = sha256();
            long size;
            try (InputStream input = new DigestInputStream(new BufferedInputStream(content), digest);
                 OutputStream output = new BufferedOutputStream(Files.newOutputStream(temporary))) {
                size = input.transferTo(output);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return new StoredDocument(size, hex(digest.digest()));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return new BufferedInputStream(Files.newInputStream(resolve(storageKey)));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return resolved;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }
}