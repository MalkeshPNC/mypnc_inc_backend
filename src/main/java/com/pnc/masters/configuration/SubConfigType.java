package com.pnc.masters.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblsub_config_types")
public class SubConfigType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "type_code", nullable = false, unique = true, length = 80)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 120)
    private String typeName;

    @Column(name = "field_1_label", length = 80)
    private String field1Label;
    @Column(name = "field_2_label", length = 80)
    private String field2Label;
    @Column(name = "field_3_label", length = 80)
    private String field3Label;
    @Column(name = "field_4_label", length = 80)
    private String field4Label;
    @Column(name = "field_5_label", length = 80)
    private String field5Label;
    @Column(name = "field_6_label", length = 80)
    private String field6Label;
    @Column(name = "field_7_label", length = 80)
    private String field7Label;
    @Column(name = "field_8_label", length = 80)
    private String field8Label;

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getFieldLabel(int index) {
        return switch (index) {
            case 1 -> field1Label;
            case 2 -> field2Label;
            case 3 -> field3Label;
            case 4 -> field4Label;
            case 5 -> field5Label;
            case 6 -> field6Label;
            case 7 -> field7Label;
            case 8 -> field8Label;
            default -> throw new IllegalArgumentException("Field index must be 1-8");
        };
    }

    public void setFieldLabel(int index, String label) {
        switch (index) {
            case 1 -> field1Label = label;
            case 2 -> field2Label = label;
            case 3 -> field3Label = label;
            case 4 -> field4Label = label;
            case 5 -> field5Label = label;
            case 6 -> field6Label = label;
            case 7 -> field7Label = label;
            case 8 -> field8Label = label;
            default -> throw new IllegalArgumentException("Field index must be 1-8");
        }
    }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
