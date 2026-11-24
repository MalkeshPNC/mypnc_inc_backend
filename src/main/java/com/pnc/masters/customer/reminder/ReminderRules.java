package com.pnc.masters.customer.reminder;

/**
 * Who the worker may claim, and which rows a later action may queue again.
 * A paused or stopped batch is never claimable. A row left in {@code sending}
 * after a restart is not put back on the queue.
 */
public final class ReminderRules {

    private ReminderRules() {
    }

    public static boolean claimable(String batchStatus, String recipientStatus) {
        return ReminderBatch.RUNNING.equals(batchStatus) && ReminderRecipient.PENDING.equals(recipientStatus);
    }

    /** Sending means SMTP may already have accepted the message. Do not send it again. */
    public static String afterRestart(String status) {
        if (ReminderRecipient.SENDING.equals(status)) {
            return ReminderRecipient.NOT_CONFIRMED;
        }
        return status;
    }

    /** Send the rest queues failed rows again. Pending is already waiting. Sent stays sent. */
    public static boolean requeueForRest(String status) {
        return ReminderRecipient.FAILED.equals(status);
    }

    /** A second email to people who already received this run. Never the default. */
    public static boolean requeueForAgain(String status) {
        return ReminderRecipient.SENT.equals(status);
    }

    public static boolean hasWork(String status) {
        return ReminderRecipient.PENDING.equals(status)
                || ReminderRecipient.SENDING.equals(status)
                || ReminderRecipient.FAILED.equals(status)
                || ReminderRecipient.NOT_CONFIRMED.equals(status);
    }
}
