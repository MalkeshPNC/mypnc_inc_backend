package com.pnc.masters.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPresenceRegistryTest {

    private final ChatPresenceRegistry presence = new ChatPresenceRegistry();

    @Test
    void firstSessionMarksOnlineAndSecondKeepsThemOnline() {
        assertThat(presence.connect(7L, "a")).isTrue();
        assertThat(presence.isOnline(7L)).isTrue();
        assertThat(presence.connectionCount(7L)).isEqualTo(1);

        assertThat(presence.connect(7L, "b")).isFalse();
        assertThat(presence.connectionCount(7L)).isEqualTo(2);

        assertThat(presence.disconnect(7L, "a")).isFalse();
        assertThat(presence.isOnline(7L)).isTrue();

        assertThat(presence.disconnect(7L, "b")).isTrue();
        assertThat(presence.isOnline(7L)).isFalse();
        assertThat(presence.status(7L)).isEqualTo(ChatPresenceRegistry.OFFLINE);
        assertThat(presence.connectionCount(7L)).isZero();
        assertThat(presence.lastSeenAt(7L)).isNotNull();
    }

    @Test
    void connectedUserCanMoveBetweenOnlineAndIdle() {
        presence.connect(3L, "tab-1");
        assertThat(presence.status(3L)).isEqualTo(ChatPresenceRegistry.ONLINE);

        assertThat(presence.setAvailability(3L, "idle")).isTrue();
        assertThat(presence.status(3L)).isEqualTo(ChatPresenceRegistry.IDLE);
        assertThat(presence.isOnline(3L)).isTrue();

        assertThat(presence.setAvailability(3L, "idle")).isFalse();
        assertThat(presence.setAvailability(3L, "offline")).isFalse();
        assertThat(presence.status(3L)).isEqualTo(ChatPresenceRegistry.IDLE);

        assertThat(presence.setAvailability(3L, "online")).isTrue();
        assertThat(presence.status(3L)).isEqualTo(ChatPresenceRegistry.ONLINE);
    }
}
