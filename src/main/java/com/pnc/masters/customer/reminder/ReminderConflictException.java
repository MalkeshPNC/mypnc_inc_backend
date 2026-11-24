package com.pnc.masters.customer.reminder;

public class ReminderConflictException extends RuntimeException {

    public ReminderConflictException(String message) {
        super(message);
    }
}
