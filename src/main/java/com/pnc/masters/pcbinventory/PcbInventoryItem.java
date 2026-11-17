package com.pnc.masters.pcbinventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tblpcb_inventory")
public class PcbInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "item_type", nullable = false, length = 40)
    private String itemType;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    @Column(name = "manufacturer_name", length = 200)
    private String manufacturerName;

    @Column(name = "sheet_size", length = 200)
    private String sheetSize;

    @Column(name = "available", nullable = false)
    private int available;

    @Column(name = "allocated", nullable = false)
    private int allocated;

    @Column(name = "on_order", nullable = false)
    private int onOrder;

    @Column(name = "lead_time", length = 80)
    private String leadTime;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(name = "reorder_qty", nullable = false)
    private int reorderQty;

    @Column(name = "alert_on_item", length = 500)
    private String alertOnItem;

    @Column(name = "std_price", precision = 12, scale = 2)
    private BigDecimal stdPrice;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Long getManufacturerId() { return manufacturerId; }
    public void setManufacturerId(Long manufacturerId) { this.manufacturerId = manufacturerId; }
    public String getManufacturerName() { return manufacturerName; }
    public void setManufacturerName(String manufacturerName) { this.manufacturerName = manufacturerName; }
    public String getSheetSize() { return sheetSize; }
    public void setSheetSize(String sheetSize) { this.sheetSize = sheetSize; }
    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }
    public int getAllocated() { return allocated; }
    public void setAllocated(int allocated) { this.allocated = allocated; }
    public int getOnOrder() { return onOrder; }
    public void setOnOrder(int onOrder) { this.onOrder = onOrder; }
    public String getLeadTime() { return leadTime; }
    public void setLeadTime(String leadTime) { this.leadTime = leadTime; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public int getReorderQty() { return reorderQty; }
    public void setReorderQty(int reorderQty) { this.reorderQty = reorderQty; }
    public String getAlertOnItem() { return alertOnItem; }
    public void setAlertOnItem(String alertOnItem) { this.alertOnItem = alertOnItem; }
    public BigDecimal getStdPrice() { return stdPrice; }
    public void setStdPrice(BigDecimal stdPrice) { this.stdPrice = stdPrice; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
