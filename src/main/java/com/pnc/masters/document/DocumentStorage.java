package com.pnc.masters.document;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorage {
    StoredDocument store(String storageKey, InputStream content) throws IOException;
    InputStream open(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;

    record StoredDocument(long size, String checksumSha256) { }
}