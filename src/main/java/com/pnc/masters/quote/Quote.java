package com.pnc.masters.quote;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tblquotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qid")
    private Long qid;

    @Column(name = "quote_number", nullable = false, unique = true, length = 80)
    private String quoteNumber;

    @Column(name = "quote_type", length = 80)
    private String quoteType;

    @Column(name = "project_number", length = 80)
    private String projectNumber;

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    @Column(name = "submit_date")
    private LocalDate submitDate;

    @Column(name = "cust_id")
    private Long custId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "cont_id")
    private Long contId;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "customer_rfq", length = 120)
    private String customerRfq;

    @Column(name = "nc_id")
    private Long ncId;

    @Column(name = "nc_number", length = 80)
    private String ncNumber;

    @Column(name = "assy_number", length = 120)
    private String assyNumber;

    @Column(name = "pcba_revision", length = 40)
    private String pcbaRevision;

    @Column(name = "pcb_number", length = 120)
    private String pcbNumber;

    @Column(name = "pcb_revision", length = 40)
    private String pcbRevision;

    @Column(name = "assy_quote_status", length = 80)
    private String assyQuoteStatus;

    @Column(name = "assy_quote_person", length = 200)
    private String assyQuotePerson;

    @Column(name = "pcb_quote_number", length = 120)
    private String pcbQuoteNumber;

    @Column(name = "pcb_quote_status", length = 80)
    private String pcbQuoteStatus;

    @Column(name = "pcb_quote_person", length = 200)
    private String pcbQuotePerson;

    // "array" is reserved in MySQL 8, so the column carries a prefix.
    @Column(name = "quote_array", length = 80)
    private String quoteArray;

    /** Rate applied to every quantity line's sub total to reach Total (S%). */
    @Column(name = "commission_percentage", precision = 6, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(name = "internal_note1", columnDefinition = "TEXT")
    private String internalNote1;

    @Column(name = "internal_note2", columnDefinition = "TEXT")
    private String internalNote2;

    @Column(name = "notes_to_customer", columnDefinition = "TEXT")
    private String notesToCustomer;

    @Column(name = "other_nre_charges", length = 200)
    private String otherNreCharges;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "pnc_notes", nullable = false)
    private boolean pncNotes = false;

    @Column(name = "labor_only", nullable = false)
    private boolean laborOnly = false;

    @Column(name = "parts_scheduled", nullable = false)
    private boolean partsScheduled = false;

    @Column(name = "feedback", nullable = false)
    private boolean feedback = false;

    @Column(name = "itarc", nullable = false)
    private boolean itarc = false;

    @Column(name = "berryc", nullable = false)
    private boolean berryc = false;

    @Column(name = "sams_review", nullable = false)
    private boolean samsReview = false;

    @Column(name = "pcba_plant", length = 120)
    private String pcbaPlant;

    @Column(name = "pcb_origin", length = 120)
    private String pcbOrigin;

    @Column(name = "fai_req_pnc", nullable = false)
    private boolean faiReqPnc = false;

    @Column(name = "fai_pcb_wo", precision = 12, scale = 2)
    private BigDecimal faiPcbWo;

    @Column(name = "fai_pcba_wo", precision = 12, scale = 2)
    private BigDecimal faiPcbaWo;

    @Column(name = "fai_heading_pnc", length = 1000)
    private String faiHeadingPnc;

    @Column(name = "fai_req_as9102", nullable = false)
    private boolean faiReqAs9102 = false;

    @Column(name = "fai_pcb_with", precision = 12, scale = 2)
    private BigDecimal faiPcbWith;

    @Column(name = "fai_pcba_with", precision = 12, scale = 2)
    private BigDecimal faiPcbaWith;

    @Column(name = "fai_heading_as9102", length = 1000)
    private String faiHeadingAs9102;

    @Column(name = "status", length = 80)
    private String status;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "quote", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("qtyId")
    private List<QuoteQuantity> quantities = new ArrayList<>();

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addQuantity(QuoteQuantity quantity) {
        quantity.setQuote(this);
        quantities.add(quantity);
    }

    public Long getQid() { return qid; }
    public void setQid(Long qid) { this.qid = qid; }
    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }
    public String getQuoteType() { return quoteType; }
    public void setQuoteType(String quoteType) { this.quoteType = quoteType; }
    public String getProjectNumber() { return projectNumber; }
    public void setProjectNumber(String projectNumber) { this.projectNumber = projectNumber; }
    public LocalDate getCreateDate() { return createDate; }
    public void setCreateDate(LocalDate createDate) { this.createDate = createDate; }
    public LocalDate getSubmitDate() { return submitDate; }
    public void setSubmitDate(LocalDate submitDate) { this.submitDate = submitDate; }
    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Long getContId() { return contId; }
    public void setContId(Long contId) { this.contId = contId; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getCustomerRfq() { return customerRfq; }
    public void setCustomerRfq(String customerRfq) { this.customerRfq = customerRfq; }
    public Long getNcId() { return ncId; }
    public void setNcId(Long ncId) { this.ncId = ncId; }
    public String getNcNumber() { return ncNumber; }
    public void setNcNumber(String ncNumber) { this.ncNumber = ncNumber; }
    public String getAssyNumber() { return assyNumber; }
    public void setAssyNumber(String assyNumber) { this.assyNumber = assyNumber; }
    public String getPcbaRevision() { return pcbaRevision; }
    public void setPcbaRevision(String pcbaRevision) { this.pcbaRevision = pcbaRevision; }
    public String getPcbNumber() { return pcbNumber; }
    public void setPcbNumber(String pcbNumber) { this.pcbNumber = pcbNumber; }
    public String getPcbRevision() { return pcbRevision; }
    public void setPcbRevision(String pcbRevision) { this.pcbRevision = pcbRevision; }
    public String getAssyQuoteStatus() { return assyQuoteStatus; }
    public void setAssyQuoteStatus(String assyQuoteStatus) { this.assyQuoteStatus = assyQuoteStatus; }
    public String getAssyQuotePerson() { return assyQuotePerson; }
    public void setAssyQuotePerson(String assyQuotePerson) { this.assyQuotePerson = assyQuotePerson; }
    public String getPcbQuoteNumber() { return pcbQuoteNumber; }
    public void setPcbQuoteNumber(String pcbQuoteNumber) { this.pcbQuoteNumber = pcbQuoteNumber; }
    public String getPcbQuoteStatus() { return pcbQuoteStatus; }
    public void setPcbQuoteStatus(String pcbQuoteStatus) { this.pcbQuoteStatus = pcbQuoteStatus; }
    public String getPcbQuotePerson() { return pcbQuotePerson; }
    public void setPcbQuotePerson(String pcbQuotePerson) { this.pcbQuotePerson = pcbQuotePerson; }
    public String getQuoteArray() { return quoteArray; }
    public void setQuoteArray(String quoteArray) { this.quoteArray = quoteArray; }
    public BigDecimal getCommissionPercentage() { return commissionPercentage; }
    public void setCommissionPercentage(BigDecimal commissionPercentage) { this.commissionPercentage = commissionPercentage; }
    public String getInternalNote1() { return internalNote1; }
    public void setInternalNote1(String internalNote1) { this.internalNote1 = internalNote1; }
    public String getInternalNote2() { return internalNote2; }
    public void setInternalNote2(String internalNote2) { this.internalNote2 = internalNote2; }
    public String getNotesToCustomer() { return notesToCustomer; }
    public void setNotesToCustomer(String notesToCustomer) { this.notesToCustomer = notesToCustomer; }
    public String getOtherNreCharges() { return otherNreCharges; }
    public void setOtherNreCharges(String otherNreCharges) { this.otherNreCharges = otherNreCharges; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public boolean isPncNotes() { return pncNotes; }
    public void setPncNotes(boolean pncNotes) { this.pncNotes = pncNotes; }
    public boolean isLaborOnly() { return laborOnly; }
    public void setLaborOnly(boolean laborOnly) { this.laborOnly = laborOnly; }
    public boolean isPartsScheduled() { return partsScheduled; }
    public void setPartsScheduled(boolean partsScheduled) { this.partsScheduled = partsScheduled; }
    public boolean isFeedback() { return feedback; }
    public void setFeedback(boolean feedback) { this.feedback = feedback; }
    public boolean isItarc() { return itarc; }
    public void setItarc(boolean itarc) { this.itarc = itarc; }
    public boolean isBerryc() { return berryc; }
    public void setBerryc(boolean berryc) { this.berryc = berryc; }
    public boolean isSamsReview() { return samsReview; }
    public void setSamsReview(boolean samsReview) { this.samsReview = samsReview; }
    public String getPcbaPlant() { return pcbaPlant; }
    public void setPcbaPlant(String pcbaPlant) { this.pcbaPlant = pcbaPlant; }
    public String getPcbOrigin() { return pcbOrigin; }
    public void setPcbOrigin(String pcbOrigin) { this.pcbOrigin = pcbOrigin; }
    public boolean isFaiReqPnc() { return faiReqPnc; }
    public void setFaiReqPnc(boolean faiReqPnc) { this.faiReqPnc = faiReqPnc; }
    public BigDecimal getFaiPcbWo() { return faiPcbWo; }
    public void setFaiPcbWo(BigDecimal faiPcbWo) { this.faiPcbWo = faiPcbWo; }
    public BigDecimal getFaiPcbaWo() { return faiPcbaWo; }
    public void setFaiPcbaWo(BigDecimal faiPcbaWo) { this.faiPcbaWo = faiPcbaWo; }
    public String getFaiHeadingPnc() { return faiHeadingPnc; }
    public void setFaiHeadingPnc(String faiHeadingPnc) { this.faiHeadingPnc = faiHeadingPnc; }
    public boolean isFaiReqAs9102() { return faiReqAs9102; }
    public void setFaiReqAs9102(boolean faiReqAs9102) { this.faiReqAs9102 = faiReqAs9102; }
    public BigDecimal getFaiPcbWith() { return faiPcbWith; }
    public void setFaiPcbWith(BigDecimal faiPcbWith) { this.faiPcbWith = faiPcbWith; }
    public BigDecimal getFaiPcbaWith() { return faiPcbaWith; }
    public void setFaiPcbaWith(BigDecimal faiPcbaWith) { this.faiPcbaWith = faiPcbaWith; }
    public String getFaiHeadingAs9102() { return faiHeadingAs9102; }
    public void setFaiHeadingAs9102(String faiHeadingAs9102) { this.faiHeadingAs9102 = faiHeadingAs9102; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedByUserId() { return updatedByUserId; }
    public void setUpdatedByUserId(Long updatedByUserId) { this.updatedByUserId = updatedByUserId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { this.isDeleted = deleted; }
    public List<QuoteQuantity> getQuantities() { return quantities; }
    public void setQuantities(List<QuoteQuantity> quantities) { this.quantities = quantities; }
}
