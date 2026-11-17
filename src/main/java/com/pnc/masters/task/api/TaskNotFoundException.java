package com.pnc.masters.task.api;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(String message) {
        super(message);
    }

    public static TaskNotFoundException task(Long id) {
        return new TaskNotFoundException("Task " + id + " was not found");
    }

    public static TaskNotFoundException file(Long id) {
        return new TaskNotFoundException("Task file " + id + " was not found");
    }
}
