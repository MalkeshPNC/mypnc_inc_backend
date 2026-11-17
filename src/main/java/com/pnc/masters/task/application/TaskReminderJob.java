package com.pnc.masters.task.application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskReminderJob {

    private final TaskService tasks;

    public TaskReminderJob(TaskService tasks) {
        this.tasks = tasks;
    }

    @Scheduled(cron = "0 */15 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void send() {
        tasks.sendDueReminders();
    }
}
