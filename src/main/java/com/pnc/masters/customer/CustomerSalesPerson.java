package com.pnc.masters.customer;

import com.pnc.masters.salesperson.SalesPerson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "tblcustomer_salespersons", uniqueConstraints = @UniqueConstraint(columnNames = {"cust_id", "sp_id"}))
public class CustomerSalesPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "csp_id")
    private Long cspId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cust_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sp_id", nullable = false)
    private SalesPerson salesPerson;

    @Column(precision = 10, scale = 2)
    private BigDecimal commission;

    public Long getCspId() { return cspId; }
    public void setCspId(Long cspId) { this.cspId = cspId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public SalesPerson getSalesPerson() { return salesPerson; }
    public void setSalesPerson(SalesPerson salesPerson) { this.salesPerson = salesPerson; }
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }
}