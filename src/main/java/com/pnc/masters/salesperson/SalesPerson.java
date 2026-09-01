package com.pnc.masters.salesperson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tblsalespersons")
public class SalesPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sp_id")
    private Long spId;

    @Column(name = "sales_person", nullable = false, length = 200)
    private String salesPerson;

    @Column(name = "sp_email", length = 320)
    private String spEmail;

    public Long getSpId() { return spId; }
    public void setSpId(Long spId) { this.spId = spId; }
    public String getSalesPerson() { return salesPerson; }
    public void setSalesPerson(String salesPerson) { this.salesPerson = salesPerson; }
    public String getSpEmail() { return spEmail; }
    public void setSpEmail(String spEmail) { this.spEmail = spEmail; }
}