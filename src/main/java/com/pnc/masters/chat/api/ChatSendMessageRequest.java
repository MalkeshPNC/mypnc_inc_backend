package com.pnc.masters.chat.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSendMessageRequest(
        @NotBlank @Size(max = 2000) String body
) {
}
