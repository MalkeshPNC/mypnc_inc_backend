package com.pnc.masters.customer.reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReminderResponses {

    private ReminderResponses() {
    }

    public record ReminderAudienceResponse(String kind, List<ReminderCustomerResponse> customers) {
    }

    public record ReminderCustomerResponse(
            Long custId,
            String name,
            int followupCount,
            LocalDateTime lastReminder,
            String followupBy,
            String state,
            LocalDate lastSubmitted,
            boolean pcbOnly,
            int announcementFollowupCount,
            LocalDateTime lastAnnouncement,
            List<ReminderContactResponse> contacts
    ) {
    }

    public record ReminderContactResponse(Long contactId, String name, String email) {
    }

    public record ReminderActiveResponse(ReminderBatchResponse batch) {
    }

    public record ReminderBatchResponse(
            Long id,
            String kind,
            String subject,
            String body,
            String cc,
            String status,
            String startedBy,
            LocalDateTime createdAt,
            List<ReminderAttachmentResponse> attachments,
            List<ReminderRecipientResponse> recipients,
            ReminderCountsResponse counts
    ) {
    }

    public record ReminderAttachmentResponse(Long id, String fileName, String contentType) {
    }

    public record ReminderRecipientResponse(
            Long id,
            Long customerId,
            String customerName,
            Long contactId,
            String contactName,
            String email,
            String status,
            String errorMessage,
            LocalDateTime sentAt
    ) {
    }

    public record ReminderCountsResponse(int pending, int sending, int sent, int failed, int notConfirmed) {
    }

    public record ReminderLogItemResponse(
            Long id,
            Long batchId,
            String kind,
            String subject,
            String cc,
            String startedBy,
            LocalDateTime createdAt,
            String customerName,
            String contactName,
            String email,
            String status,
            String errorMessage,
            LocalDateTime sentAt
    ) {
    }

    public record ReminderLogPageResponse(
            List<ReminderLogItemResponse> items,
            long total,
            int page,
            int size
    ) {
    }

    public record ReminderLogDetailResponse(
            Long id,
            Long batchId,
            String kind,
            String subject,
            String body,
            String cc,
            String startedBy,
            LocalDateTime createdAt,
            String customerName,
            String contactName,
            String email,
            String status,
            String errorMessage,
            LocalDateTime sentAt
    ) {
    }
}
