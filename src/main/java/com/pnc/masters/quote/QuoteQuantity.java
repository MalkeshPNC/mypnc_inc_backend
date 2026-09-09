package com.pnc.masters.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tblquote_quantities")
public class QuoteQuantity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qty_id")
    private Long qtyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qid", nullable = false)
    private Quote quote;

    @Column(name = "lt", length = 40)
    private String lt;

    @Column(name = "lt_pcb")
    private Integer ltPcb;

    @Column(name = "lt_pcba")
    private Integer ltPcba;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "pcb_cost", precision = 12, scale = 2)
    private BigDecimal pcbCost;

    @Column(name = "parts_cost", precision = 12, scale = 2)
    private BigDecimal partsCost;

    @Column(name = "labor_cost", precision = 12, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "labor_discount", precision = 6, scale = 2)
    private BigDecimal laborDiscount;

    @Column(name = "part_markup", precision = 6, scale = 2)
    private BigDecimal partMarkup;

    @Column(name = "testing", precision = 12, scale = 2)
    private BigDecimal testing;

    @Column(name = "conf_coat", precision = 12, scale = 2)
    private BigDecimal confCoat;

    @Column(name = "service_name", length = 120)
    private String serviceName;

    @Column(name = "service_charge", precision = 12, scale = 2)
    private BigDecimal serviceCharge;

    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "total_sales_pct", precision = 12, scale = 2)
    private BigDecimal totalSalesPct;

    @Column(name = "pcb_nre", precision = 12, scale = 2)
    private BigDecimal pcbNre;

    @Column(name = "assy_nre", precision = 12, scale = 2)
    private BigDecimal assyNre;

    @Column(name = "stencil", precision = 12, scale = 2)
    private BigDecimal stencil;

    @Column(name = "other_nre", precision = 12, scale = 2)
    private BigDecimal otherNre;

    @Column(name = "total", precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "received", nullable = false)
    private boolean received = false;

    public Long getQtyId() { return qtyId; }
    public void setQtyId(Long qtyId) { this.qtyId = qtyId; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public String getLt() { return lt; }
    public void setLt(String lt) { this.lt = lt; }
    public Integer getLtPcb() { return ltPcb; }
    public void setLtPcb(Integer ltPcb) { this.ltPcb = ltPcb; }
    public Integer getLtPcba() { return ltPcba; }
    public void setLtPcba(Integer ltPcba) { this.ltPcba = ltPcba; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getPcbCost() { return pcbCost; }
    public void setPcbCost(BigDecimal pcbCost) { this.pcbCost = pcbCost; }
    public BigDecimal getPartsCost() { return partsCost; }
    public void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }
    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
    public BigDecimal getLaborDiscount() { return laborDiscount; }
    public void setLaborDiscount(BigDecimal laborDiscount) { this.laborDiscount = laborDiscount; }
    public BigDecimal getPartMarkup() { return partMarkup; }
    public void setPartMarkup(BigDecimal partMarkup) { this.partMarkup = partMarkup; }
    public BigDecimal getTesting() { return testing; }
    public void setTesting(BigDecimal testing) { this.testing = testing; }
    public BigDecimal getConfCoat() { return confCoat; }
    public void setConfCoat(BigDecimal confCoat) { this.confCoat = confCoat; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public BigDecimal getServiceCharge() { return serviceCharge; }
    public void setServiceCharge(BigDecimal serviceCharge) { this.serviceCharge = serviceCharge; }
    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }
    public BigDecimal getTotalSalesPct() { return totalSalesPct; }
    public void setTotalSalesPct(BigDecimal totalSalesPct) { this.totalSalesPct = totalSalesPct; }
    public BigDecimal getPcbNre() { return pcbNre; }
    public void setPcbNre(BigDecimal pcbNre) { this.pcbNre = pcbNre; }
    public BigDecimal getAssyNre() { return assyNre; }
    public void setAssyNre(BigDecimal assyNre) { this.assyNre = assyNre; }
    public BigDecimal getStencil() { return stencil; }
    public void setStencil(BigDecimal stencil) { this.stencil = stencil; }
    public BigDecimal getOtherNre() { return otherNre; }
    public void setOtherNre(BigDecimal otherNre) { this.otherNre = otherNre; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public boolean isReceived() { return received; }
    public void setReceived(boolean received) { this.received = received; }
}
