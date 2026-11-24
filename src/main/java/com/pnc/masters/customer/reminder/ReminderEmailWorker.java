package com.pnc.masters.customer.reminder;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderEmailWorker {

    private final ReminderEmailService emails;

    public ReminderEmailWorker(ReminderEmailService emails) {
        this.emails = emails;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void releaseInterrupted() {
        emails.releaseStuck();
    }

    @Scheduled(fixedDelay = 2000)
    public void sendNext() {
        Long id = emails.claimNext();
        if (id != null) {
            emails.dispatch(id);
        }
    }
}
