package com.pnc.masters.customer;

import com.pnc.masters.contact.Contact;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tblcustomers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cust_id")
    private Long custId;

    @Column(nullable = false, length = 200)
    private String customer;

    @Column(name = "company_logo", length = 500)
    private String companyLogo;

    @Column(name = "salesperson_default_commission", precision = 10, scale = 2)
    private BigDecimal salesPersonDefaultCommission;

    @Column(name = "cust_entry_dt", nullable = false)
    private LocalDateTime custEntryDt;

    @Column(name = "referred_by", length = 200)
    private String referredBy;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 500)
    private String address;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String state;

    @Column(length = 20)
    private String zip;

    @Column(name = "billto_address", length = 500)
    private String billtoAddress;

    @Column(name = "shipto_address", length = 500)
    private String shiptoAddress;

    @Column(name = "automail_on", nullable = false)
    private boolean automailOn;

    @Column(name = "isDeleted", nullable = false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Contact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CustomerSalesPerson> salesPersons = new ArrayList<>();

    @PrePersist
    void setEntryDate() {
        if (custEntryDt == null) {
            custEntryDt = LocalDateTime.now();
        }
    }

    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public String getCompanyLogo() { return companyLogo; }
    public void setCompanyLogo(String companyLogo) { this.companyLogo = companyLogo; }
    public BigDecimal getSalesPersonDefaultCommission() { return salesPersonDefaultCommission; }
    public void setSalesPersonDefaultCommission(BigDecimal salesPersonDefaultCommission) {
        this.salesPersonDefaultCommission = salesPersonDefaultCommission;
    }
    public LocalDateTime getCustEntryDt() { return custEntryDt; }
    public void setCustEntryDt(LocalDateTime custEntryDt) { this.custEntryDt = custEntryDt; }
    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getBilltoAddress() { return billtoAddress; }
    public void setBilltoAddress(String billtoAddress) { this.billtoAddress = billtoAddress; }
    public String getShiptoAddress() { return shiptoAddress; }
    public void setShiptoAddress(String shiptoAddress) { this.shiptoAddress = shiptoAddress; }
    public boolean isAutomailOn() { return automailOn; }
    public void setAutomailOn(boolean automailOn) { this.automailOn = automailOn; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public List<Contact> getContacts() { return contacts; }
    public void setContacts(List<Contact> contacts) { this.contacts = contacts; }
    public List<CustomerSalesPerson> getSalesPersons() { return salesPersons; }
    public void setSalesPersons(List<CustomerSalesPerson> salesPersons) { this.salesPersons = salesPersons; }
}
