package com.pnc.masters.task.api;

public record TaskProgressRequest(String status, String comment, boolean commentSpecified) {
}
