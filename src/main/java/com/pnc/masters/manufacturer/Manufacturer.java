package com.pnc.masters.manufacturer;

import com.pnc.masters.party.PartyRow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblmanufacturers")
public class Manufacturer implements PartyRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "type_entry_id", nullable = false)
    private Long typeEntryId;

    @Column(name = "subtype_entry_id")
    private Long subtypeEntryId;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_address", length = 500)
    private String contactAddress;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Override public Long getId() { return manufacturerId; }
    @Override public void setId(Long id) { this.manufacturerId = id; }
    @Override public String getCode() { return code; }
    @Override public void setCode(String code) { this.code = code; }
    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public Long getTypeEntryId() { return typeEntryId; }
    @Override public void setTypeEntryId(Long typeEntryId) { this.typeEntryId = typeEntryId; }
    @Override public Long getSubtypeEntryId() { return subtypeEntryId; }
    @Override public void setSubtypeEntryId(Long subtypeEntryId) { this.subtypeEntryId = subtypeEntryId; }
    @Override public String getContactName() { return contactName; }
    @Override public void setContactName(String contactName) { this.contactName = contactName; }
    @Override public String getContactAddress() { return contactAddress; }
    @Override public void setContactAddress(String contactAddress) { this.contactAddress = contactAddress; }
    @Override public String getContactEmail() { return contactEmail; }
    @Override public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    @Override public String getContactPhone() { return contactPhone; }
    @Override public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    @Override public Long getCreatedBy() { return createdBy; }
    @Override public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    @Override public LocalDateTime getCreatedDate() { return createdDate; }
    @Override public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    @Override public Long getUpdatedBy() { return updatedBy; }
    @Override public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    @Override public LocalDateTime getUpdatedDate() { return updatedDate; }
    @Override public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    @Override public boolean isDeleted() { return deleted; }
    @Override public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
