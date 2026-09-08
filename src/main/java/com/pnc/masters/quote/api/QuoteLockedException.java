package com.pnc.masters.quote.api;

/**
 * Raised when a caller tries to save a quote it does not hold the edit lock
 * for, either because someone else took it or because their own lock went idle
 * and expired.
 */
public class QuoteLockedException extends RuntimeException {

    private final Long qid;
    private final Long lockedByUserId;
    private final String lockedByName;

    public QuoteLockedException(Long qid, Long lockedByUserId, String lockedByName) {
        super(lockedByName == null
                ? "Your editing session for quote " + qid + " expired. Reopen the quote to continue."
                : "Quote " + qid + " is being edited by " + lockedByName + ".");
        this.qid = qid;
        this.lockedByUserId = lockedByUserId;
        this.lockedByName = lockedByName;
    }

    public Long getQid() { return qid; }
    public Long getLockedByUserId() { return lockedByUserId; }
    public String getLockedByName() { return lockedByName; }
}
