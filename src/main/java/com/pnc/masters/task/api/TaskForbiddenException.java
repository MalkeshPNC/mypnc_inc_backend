package com.pnc.masters.task.api;

public class TaskForbiddenException extends RuntimeException {

    public TaskForbiddenException(String message) {
        super(message);
    }
}
