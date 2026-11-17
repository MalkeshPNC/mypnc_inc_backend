package com.pnc.masters.chat.api;

import jakarta.validation.constraints.NotNull;

public record ChatCreateConversationRequest(@NotNull Long peerUserId) {
}
