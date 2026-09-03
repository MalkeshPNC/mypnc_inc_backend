package com.pnc.masters.ncmaster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblnc_masters")
public class NcMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nc_id")
    private Long ncId;

    @Column(name = "nc_number", nullable = false, unique = true, length = 80)
    private String ncNumber;

    @Column(name = "pcb_part_number", length = 120)
    private String pcbPartNumber;

    @Column(name = "pcb_rev", length = 40)
    private String pcbRev;

    @Column(name = "pcba_part_number", length = 120)
    private String pcbaPartNumber;

    @Column(name = "pcba_rev", length = 40)
    private String pcbaRev;

    @Column(name = "pcba_alert", columnDefinition = "TEXT")
    private String pcbaAlert;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "nc_alert", columnDefinition = "TEXT")
    private String ncAlert;

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getNcId() { return ncId; }
    public void setNcId(Long ncId) { this.ncId = ncId; }
    public String getNcNumber() { return ncNumber; }
    public void setNcNumber(String ncNumber) { this.ncNumber = ncNumber; }
    public String getPcbPartNumber() { return pcbPartNumber; }
    public void setPcbPartNumber(String pcbPartNumber) { this.pcbPartNumber = pcbPartNumber; }
    public String getPcbRev() { return pcbRev; }
    public void setPcbRev(String pcbRev) { this.pcbRev = pcbRev; }
    public String getPcbaPartNumber() { return pcbaPartNumber; }
    public void setPcbaPartNumber(String pcbaPartNumber) { this.pcbaPartNumber = pcbaPartNumber; }
    public String getPcbaRev() { return pcbaRev; }
    public void setPcbaRev(String pcbaRev) { this.pcbaRev = pcbaRev; }
    public String getPcbaAlert() { return pcbaAlert; }
    public void setPcbaAlert(String pcbaAlert) { this.pcbaAlert = pcbaAlert; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getNcAlert() { return ncAlert; }
    public void setNcAlert(String ncAlert) { this.ncAlert = ncAlert; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
