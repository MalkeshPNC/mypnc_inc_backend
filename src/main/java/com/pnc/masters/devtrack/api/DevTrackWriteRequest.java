package com.pnc.masters.devtrack.api;

import java.time.LocalDate;

public record DevTrackWriteRequest(String name, String description, LocalDate eta, String status) {
}
