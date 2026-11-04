package com.pnc.masters.task.api;

public record TaskFileResponse(Long fileId, String originalFilename, long fileSize) {
}
