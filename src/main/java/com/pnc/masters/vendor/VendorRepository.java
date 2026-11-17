package com.pnc.masters.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    List<Vendor> findByDeletedFalseOrderByCreatedDateDescVendorIdDesc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndVendorIdNot(String code, Long vendorId);

    @Query("select count(v) from Vendor v where v.typeEntryId = :entryId or v.subtypeEntryId = :entryId")
    long countReferencing(@Param("entryId") Long entryId);
}
