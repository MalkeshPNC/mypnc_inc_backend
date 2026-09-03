package com.pnc.masters.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblsub_config_entries")
public class SubConfigEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long entryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private SubConfigType type;

    @Column(name = "field_1", length = 255)
    private String field1;
    @Column(name = "field_2", length = 255)
    private String field2;
    @Column(name = "field_3", length = 255)
    private String field3;
    @Column(name = "field_4", length = 255)
    private String field4;
    @Column(name = "field_5", length = 255)
    private String field5;
    @Column(name = "field_6", length = 255)
    private String field6;
    @Column(name = "field_7", length = 255)
    private String field7;
    @Column(name = "field_8", length = 255)
    private String field8;

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

    public String getFieldValue(int index) {
        return switch (index) {
            case 1 -> field1;
            case 2 -> field2;
            case 3 -> field3;
            case 4 -> field4;
            case 5 -> field5;
            case 6 -> field6;
            case 7 -> field7;
            case 8 -> field8;
            default -> throw new IllegalArgumentException("Field index must be 1-8");
        };
    }

    public void setFieldValue(int index, String value) {
        switch (index) {
            case 1 -> field1 = value;
            case 2 -> field2 = value;
            case 3 -> field3 = value;
            case 4 -> field4 = value;
            case 5 -> field5 = value;
            case 6 -> field6 = value;
            case 7 -> field7 = value;
            case 8 -> field8 = value;
            default -> throw new IllegalArgumentException("Field index must be 1-8");
        }
    }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }
    public SubConfigType getType() { return type; }
    public void setType(SubConfigType type) { this.type = type; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
