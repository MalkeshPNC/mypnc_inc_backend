package com.pnc.masters.party;

import java.time.LocalDateTime;

public interface PartyRow {

    Long getId();
    void setId(Long id);
    String getCode();
    void setCode(String code);
    String getName();
    void setName(String name);
    Long getTypeEntryId();
    void setTypeEntryId(Long typeEntryId);
    Long getSubtypeEntryId();
    void setSubtypeEntryId(Long subtypeEntryId);
    String getContactName();
    void setContactName(String contactName);
    String getContactAddress();
    void setContactAddress(String contactAddress);
    String getContactEmail();
    void setContactEmail(String contactEmail);
    String getContactPhone();
    void setContactPhone(String contactPhone);
    Long getCreatedBy();
    void setCreatedBy(Long createdBy);
    LocalDateTime getCreatedDate();
    void setCreatedDate(LocalDateTime createdDate);
    Long getUpdatedBy();
    void setUpdatedBy(Long updatedBy);
    LocalDateTime getUpdatedDate();
    void setUpdatedDate(LocalDateTime updatedDate);
    boolean isDeleted();
    void setDeleted(boolean deleted);
}
