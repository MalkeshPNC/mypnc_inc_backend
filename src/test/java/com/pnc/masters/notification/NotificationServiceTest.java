package com.pnc.masters.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T08:00:00Z");

    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationDelivery delivery;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, delivery, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listKeepsOnlyTheLastThreeDays() {
        when(repository.findTop50ByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                eq(7L), any())).thenReturn(List.of());

        service.list(7L);

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findTop50ByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                eq(7L), cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 18, 8, 0));
    }

    @Test
    void purgeHidesAfterThreeDaysAndDeletesAfterOneMonth() {
        service.purge();

        verify(repository).softDeleteOlderThan(
                LocalDateTime.of(2026, 10, 18, 8, 0),
                LocalDateTime.of(2026, 10, 21, 8, 0));
        verify(repository).deleteCreatedBefore(LocalDateTime.of(2026, 9, 21, 8, 0));
    }
}
