package com.pnc.masters.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One row per quote currently being edited. Separate from {@link Quote} so a
 * heartbeat never touches the quote row itself.
 */
@Entity
@Table(name = "tblquote_locks")
public class QuoteLock {

    @Id
    @Column(name = "qid")
    private Long qid;

    @Column(name = "locked_by_user_id", nullable = false)
    private Long lockedByUserId;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    public Long getQid() { return qid; }
    public void setQid(Long qid) { this.qid = qid; }
    public Long getLockedByUserId() { return lockedByUserId; }
    public void setLockedByUserId(Long lockedByUserId) { this.lockedByUserId = lockedByUserId; }
    public LocalDateTime getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(LocalDateTime acquiredAt) { this.acquiredAt = acquiredAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
