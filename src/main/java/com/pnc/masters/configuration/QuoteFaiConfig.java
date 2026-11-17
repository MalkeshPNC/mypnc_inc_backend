package com.pnc.masters.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblquote_fai_config")
public class QuoteFaiConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "fai_id")
    private Long faiId = SINGLETON_ID;

    @Column(name = "heading_notes", columnDefinition = "TEXT")
    private String headingNotes;

    @Column(name = "important_notes", columnDefinition = "TEXT")
    private String importantNotes;

    @Column(name = "pnc_fai_pcb", nullable = false, precision = 12, scale = 2)
    private BigDecimal pncFaiPcb;

    @Column(name = "pnc_fai_pcba", nullable = false, precision = 12, scale = 2)
    private BigDecimal pncFaiPcba;

    @Column(name = "as9102_fai_pcb", nullable = false, precision = 12, scale = 2)
    private BigDecimal as9102FaiPcb;

    @Column(name = "as9102_fai_pcba", nullable = false, precision = 12, scale = 2)
    private BigDecimal as9102FaiPcba;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 200)
    private String updatedBy;

    public Long getFaiId() { return faiId; }
    public void setFaiId(Long faiId) { this.faiId = faiId; }
    public String getHeadingNotes() { return headingNotes; }
    public void setHeadingNotes(String headingNotes) { this.headingNotes = headingNotes; }
    public String getImportantNotes() { return importantNotes; }
    public void setImportantNotes(String importantNotes) { this.importantNotes = importantNotes; }
    public BigDecimal getPncFaiPcb() { return pncFaiPcb; }
    public void setPncFaiPcb(BigDecimal pncFaiPcb) { this.pncFaiPcb = pncFaiPcb; }
    public BigDecimal getPncFaiPcba() { return pncFaiPcba; }
    public void setPncFaiPcba(BigDecimal pncFaiPcba) { this.pncFaiPcba = pncFaiPcba; }
    public BigDecimal getAs9102FaiPcb() { return as9102FaiPcb; }
    public void setAs9102FaiPcb(BigDecimal as9102FaiPcb) { this.as9102FaiPcb = as9102FaiPcb; }
    public BigDecimal getAs9102FaiPcba() { return as9102FaiPcba; }
    public void setAs9102FaiPcba(BigDecimal as9102FaiPcba) { this.as9102FaiPcba = as9102FaiPcba; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
