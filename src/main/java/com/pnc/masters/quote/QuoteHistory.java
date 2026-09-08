package com.pnc.masters.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Append-only log of quote saves. Kept out of the {@link Quote} aggregate so a
 * save never rewrites past rows.
 */
@Entity
@Table(name = "tblquote_history")
public class QuoteHistory {

    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qh_id")
    private Long qhId;

    @Column(name = "qid", nullable = false)
    private Long qid;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "status", length = 80)
    private String status;

    @Column(name = "assy_quote_status", length = 80)
    private String assyQuoteStatus;

    @Column(name = "pcb_quote_status", length = 80)
    private String pcbQuoteStatus;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "changed_by_name", nullable = false, length = 200)
    private String changedByName;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void setChangedAt() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    public Long getQhId() { return qhId; }
    public void setQhId(Long qhId) { this.qhId = qhId; }
    public Long getQid() { return qid; }
    public void setQid(Long qid) { this.qid = qid; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssyQuoteStatus() { return assyQuoteStatus; }
    public void setAssyQuoteStatus(String assyQuoteStatus) { this.assyQuoteStatus = assyQuoteStatus; }
    public String getPcbQuoteStatus() { return pcbQuoteStatus; }
    public void setPcbQuoteStatus(String pcbQuoteStatus) { this.pcbQuoteStatus = pcbQuoteStatus; }
    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
