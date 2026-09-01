package com.pnc.masters.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFolderDocumentStorageTest {

    @TempDir
    Path temporaryRoot;

    @Test
    void storesAndOpensBytesWithSha256Checksum() throws Exception {
        LocalFolderDocumentStorage storage = new LocalFolderDocumentStorage();
        storage.setRoot(temporaryRoot);
        byte[] content = "document bytes".getBytes();

        DocumentStorage.StoredDocument stored = storage.store("42/contract/42_contract_abcd_scan.pdf", new ByteArrayInputStream(content));

        assertThat(stored.size()).isEqualTo(content.length);
        assertThat(storage.open("42/contract/42_contract_abcd_scan.pdf").readAllBytes()).containsExactly(content);
        assertThat(stored.checksumSha256())
                .isEqualTo(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content)));
    }

    @Test
    void rejectsStorageKeysOutsideTheConfiguredRoot() {
        LocalFolderDocumentStorage storage = new LocalFolderDocumentStorage();
        storage.setRoot(temporaryRoot);

        assertThatThrownBy(() -> storage.open("../outside"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid storage key");
    }
}