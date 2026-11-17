package com.pnc.masters.pcbinventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PcbInventoryItemRepository extends JpaRepository<PcbInventoryItem, Long> {

    List<PcbInventoryItem> findAllByOrderByItemIdDesc();

    @Query("""
            SELECT i FROM PcbInventoryItem i
            WHERE i.reorderLevel > 0 AND i.available <= i.reorderLevel AND i.onOrder = 0
            ORDER BY i.itemId DESC
            """)
    List<PcbInventoryItem> findReorder();

    @Query("""
            SELECT i FROM PcbInventoryItem i
            WHERE i.onOrder > 0
            ORDER BY i.itemId DESC
            """)
    List<PcbInventoryItem> findReceiving();
}
