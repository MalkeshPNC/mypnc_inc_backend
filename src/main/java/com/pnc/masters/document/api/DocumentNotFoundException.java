package com.pnc.masters.document.api;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
    }
}