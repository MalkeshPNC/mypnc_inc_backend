package com.pnc.masters.customer.reminder;

import java.util.List;

public final class ReminderRequests {

    private ReminderRequests() {
    }

    public record ReminderBatchRequest(
            String kind,
            String subject,
            String body,
            String cc,
            List<ReminderRecipientRequest> recipients
    ) {
    }

    public record ReminderRecipientRequest(
            Long customerId,
            Long contactId,
            String email,
            String contactName
    ) {
    }

    public record ReminderReviseRequest(
            String subject,
            String body,
            String cc,
            List<Long> keepAttachmentIds,
            Boolean confirm
    ) {
    }
}
