package com.pnc.masters.quote.application;

import com.pnc.masters.quote.QuoteLock;
import com.pnc.masters.quote.QuoteLockProperties;
import com.pnc.masters.quote.QuoteLockRepository;
import com.pnc.masters.quote.api.QuoteLockResponse;
import com.pnc.masters.quote.api.QuoteLockedException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteLockServiceTest {

    private static final long TIMEOUT_MS = 300_000L;

    @Mock
    private QuoteLockRepository lockRepository;

    @Mock
    private AppUserRepository userRepository;

    private QuoteLockService service;

    @BeforeEach
    void setUp() {
        QuoteLockProperties properties = new QuoteLockProperties();
        properties.setIdleTimeoutMs(TIMEOUT_MS);
        service = new QuoteLockService(lockRepository, userRepository, properties);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Ada Lovelace")));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Grace Hopper")));
    }

    @Test
    void acquireCreatesLockWhenQuoteIsFree() {
        when(lockRepository.findByQid(12L)).thenReturn(Optional.empty());
        when(lockRepository.save(any(QuoteLock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteLockResponse response = service.acquireOrRenew(12L, 1L);

        assertThat(response.heldByCurrentUser()).isTrue();
        assertThat(response.lockedByUserId()).isEqualTo(1L);
        assertThat(response.lockedByName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void acquireRenewsWithoutMovingAcquiredAtWhenCallerAlreadyHoldsIt() {
        LocalDateTime acquired = LocalDateTime.now().minusMinutes(2);
        QuoteLock existing = lock(12L, 1L, acquired, LocalDateTime.now().minusMinutes(1));
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));
        when(lockRepository.save(existing)).thenReturn(existing);

        QuoteLockResponse response = service.acquireOrRenew(12L, 1L);

        assertThat(response.heldByCurrentUser()).isTrue();
        assertThat(existing.getAcquiredAt()).isEqualTo(acquired);
        assertThat(existing.getLastSeenAt()).isAfter(acquired);
    }

    @Test
    void acquireReportsOtherHolderWithoutTakingLiveLock() {
        QuoteLock existing = lock(12L, 2L, LocalDateTime.now(), LocalDateTime.now());
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));

        QuoteLockResponse response = service.acquireOrRenew(12L, 1L);

        assertThat(response.heldByCurrentUser()).isFalse();
        assertThat(response.lockedByName()).isEqualTo("Grace Hopper");
        verify(lockRepository, never()).save(any(QuoteLock.class));
    }

    @Test
    void acquireTakesOverLockThatWentIdle() {
        LocalDateTime stale = LocalDateTime.now().minusMinutes(10);
        QuoteLock existing = lock(12L, 2L, stale, stale);
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));
        when(lockRepository.save(existing)).thenReturn(existing);

        QuoteLockResponse response = service.acquireOrRenew(12L, 1L);

        assertThat(response.heldByCurrentUser()).isTrue();
        assertThat(existing.getLockedByUserId()).isEqualTo(1L);
        assertThat(existing.getAcquiredAt()).isAfter(stale);
    }

    @Test
    void requireHeldByPassesForTheHolderAndBumpsLastSeen() {
        LocalDateTime lastSeen = LocalDateTime.now().minusMinutes(1);
        QuoteLock existing = lock(12L, 1L, lastSeen, lastSeen);
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));

        service.requireHeldBy(12L, 1L);

        assertThat(existing.getLastSeenAt()).isAfter(lastSeen);
        verify(lockRepository).save(existing);
    }

    @Test
    void requireHeldByRejectsWhenSomebodyElseHoldsIt() {
        QuoteLock existing = lock(12L, 2L, LocalDateTime.now(), LocalDateTime.now());
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.requireHeldBy(12L, 1L))
                .isInstanceOf(QuoteLockedException.class)
                .hasMessageContaining("Grace Hopper");
    }

    @Test
    void requireHeldByRejectsWhenTheCallersOwnLockExpired() {
        LocalDateTime stale = LocalDateTime.now().minusMinutes(10);
        QuoteLock existing = lock(12L, 1L, stale, stale);
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.requireHeldBy(12L, 1L))
                .isInstanceOf(QuoteLockedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void releaseOnlyRemovesTheCallersOwnLock() {
        QuoteLock existing = lock(12L, 2L, LocalDateTime.now(), LocalDateTime.now());
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));

        service.release(12L, 1L);

        verify(lockRepository, never()).delete(existing);
    }

    @Test
    void forceReleaseRemovesAnyLock() {
        QuoteLock existing = lock(12L, 2L, LocalDateTime.now(), LocalDateTime.now());
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(existing));

        service.forceRelease(12L);

        verify(lockRepository).delete(existing);
    }

    @Test
    void findIgnoresExpiredLocks() {
        LocalDateTime stale = LocalDateTime.now().minusMinutes(10);
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(lock(12L, 2L, stale, stale)));

        assertThat(service.find(12L, 1L)).isNull();
    }

    @Test
    void findAllReturnsOnlyLiveLocksKeyedByQuote() {
        LocalDateTime stale = LocalDateTime.now().minusMinutes(10);
        when(lockRepository.findAllByQidIn(List.of(12L, 13L))).thenReturn(List.of(
                lock(12L, 2L, LocalDateTime.now(), LocalDateTime.now()),
                lock(13L, 2L, stale, stale)
        ));

        var locks = service.findAll(List.of(12L, 13L), 1L);

        assertThat(locks).containsOnlyKeys(12L);
        assertThat(locks.get(12L).lockedByName()).isEqualTo("Grace Hopper");
    }

    private static QuoteLock lock(Long qid, Long userId, LocalDateTime acquiredAt, LocalDateTime lastSeenAt) {
        QuoteLock lock = new QuoteLock();
        lock.setQid(qid);
        lock.setLockedByUserId(userId);
        lock.setAcquiredAt(acquiredAt);
        lock.setLastSeenAt(lastSeenAt);
        return lock;
    }

    private static AppUser user(Long id, String displayName) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setDisplayName(displayName);
        return user;
    }
}
