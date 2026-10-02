package com.pnc.masters.quote.application;

import com.pnc.masters.quote.QuoteLock;
import com.pnc.masters.quote.QuoteLockProperties;
import com.pnc.masters.quote.QuoteLockRepository;
import com.pnc.masters.quote.api.QuoteLockNotice;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteTakeoverServiceTest {

    @Mock
    private QuoteLockRepository lockRepository;
    @Mock
    private AppUserRepository users;
    @Mock
    private QuoteLockNotifier notifier;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-02T10:00:00Z"));
    private QuoteTakeoverService service;

    @BeforeEach
    void setUp() {
        QuoteLockProperties properties = new QuoteLockProperties();
        properties.setIdleTimeoutMs(300_000L);
        QuoteLockService locks = new QuoteLockService(lockRepository, users, properties);
        service = new QuoteTakeoverService(locks, users, notifier, clock);
    }

    @Test
    void denyTellsTheAdminAndLeavesTheLock() {
        when(lockRepository.findByQid(4L)).thenReturn(Optional.of(heldBy(8L)));
        when(notifier.isConnected(8L)).thenReturn(true);
        when(users.findById(8L)).thenReturn(Optional.of(user("Paresh")));
        when(users.findById(1L)).thenReturn(Optional.of(user("Admin")));

        service.request(4L, 1L);
        service.respond(4L, 8L, false);

        ArgumentCaptor<QuoteLockNotice> notice = ArgumentCaptor.forClass(QuoteLockNotice.class);
        verify(notifier).send(org.mockito.ArgumentMatchers.eq(1L), notice.capture());
        assertThat(notice.getValue().type()).isEqualTo("denied");
        assertThat(notice.getValue().holderName()).isEqualTo("Paresh");
        verify(lockRepository, never()).save(any());
    }

    @Test
    void allowSavesThenHandsTheQuoteToTheAdmin() {
        when(lockRepository.findByQid(4L)).thenReturn(Optional.of(heldBy(8L)));
        when(notifier.isConnected(8L)).thenReturn(true);
        when(users.findById(8L)).thenReturn(Optional.of(user("Paresh")));
        when(users.findById(1L)).thenReturn(Optional.of(user("Admin")));

        service.request(4L, 1L);
        service.respond(4L, 8L, true);
        clock.plus(QuoteTakeoverService.SAVE_WINDOW);
        service.tick();
        service.saved(4L, 8L);

        ArgumentCaptor<QuoteLock> saved = ArgumentCaptor.forClass(QuoteLock.class);
        verify(lockRepository).save(saved.capture());
        assertThat(saved.getValue().getLockedByUserId()).isEqualTo(1L);
    }

    @Test
    void missingSocketDoesNotStoreARequest() {
        when(lockRepository.findByQid(4L)).thenReturn(Optional.of(heldBy(8L)));
        when(users.findById(8L)).thenReturn(Optional.of(user("Paresh")));
        when(notifier.isConnected(8L)).thenReturn(false);

        assertThatThrownBy(() -> service.request(4L, 1L))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessageContaining("not connected");
        verify(notifier, never()).send(any(), any());
    }

    private static QuoteLock heldBy(Long userId) {
        QuoteLock lock = new QuoteLock();
        lock.setQid(4L);
        lock.setLockedByUserId(userId);
        lock.setAcquiredAt(LocalDateTime.now());
        lock.setLastSeenAt(LocalDateTime.now());
        return lock;
    }

    private static AppUser user(String name) {
        AppUser user = new AppUser();
        user.setDisplayName(name);
        return user;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void plus(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
