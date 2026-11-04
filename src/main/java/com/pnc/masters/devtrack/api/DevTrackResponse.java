package com.pnc.masters.devtrack.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DevTrackResponse(
        Long trackId,
        String name,
        String description,
        LocalDate eta,
        String status,
        LocalDateTime statusUpdatedAt,
        Long createdByUserId,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DevTrackFileResponse> files,
        List<DevTrackCommentResponse> comments
) {
}
