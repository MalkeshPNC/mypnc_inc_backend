package com.pnc.masters.pcbinventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblpcb_inventory_txn")
public class PcbInventoryTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Long txnId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "trans_type", nullable = false, length = 20)
    private String transType;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "source_name", columnDefinition = "TEXT")
    private String sourceName;

    @Column(name = "source_number", length = 120)
    private String sourceNumber;

    @Column(name = "in_qty", nullable = false)
    private int inQty;

    @Column(name = "out_qty", nullable = false)
    private int outQty;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "initial_name", length = 200)
    private String initialName;

    @Column(name = "action_by", length = 200)
    private String actionBy;

    @Column(name = "action_date", nullable = false)
    private LocalDateTime actionDate;

    public Long getTxnId() { return txnId; }
    public void setTxnId(Long txnId) { this.txnId = txnId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getTransType() { return transType; }
    public void setTransType(String transType) { this.transType = transType; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceNumber() { return sourceNumber; }
    public void setSourceNumber(String sourceNumber) { this.sourceNumber = sourceNumber; }
    public int getInQty() { return inQty; }
    public void setInQty(int inQty) { this.inQty = inQty; }
    public int getOutQty() { return outQty; }
    public void setOutQty(int outQty) { this.outQty = outQty; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getInitialName() { return initialName; }
    public void setInitialName(String initialName) { this.initialName = initialName; }
    public String getActionBy() { return actionBy; }
    public void setActionBy(String actionBy) { this.actionBy = actionBy; }
    public LocalDateTime getActionDate() { return actionDate; }
    public void setActionDate(LocalDateTime actionDate) { this.actionDate = actionDate; }
}
