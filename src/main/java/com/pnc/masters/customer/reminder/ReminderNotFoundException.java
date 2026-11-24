package com.pnc.masters.customer.reminder;

public class ReminderNotFoundException extends RuntimeException {

    public ReminderNotFoundException(Long id) {
        super("Reminder batch " + id + " was not found");
    }

    public ReminderNotFoundException(String message) {
        super(message);
    }
}
