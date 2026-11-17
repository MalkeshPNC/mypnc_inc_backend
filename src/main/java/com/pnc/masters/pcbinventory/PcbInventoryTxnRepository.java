package com.pnc.masters.pcbinventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PcbInventoryTxnRepository extends JpaRepository<PcbInventoryTxn, Long> {

    List<PcbInventoryTxn> findByItemIdOrderByActionDateDescTxnIdDesc(Long itemId);
}
