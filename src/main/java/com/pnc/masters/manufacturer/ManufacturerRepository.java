package com.pnc.masters.manufacturer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {

    List<Manufacturer> findByDeletedFalseOrderByCreatedDateDescManufacturerIdDesc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndManufacturerIdNot(String code, Long manufacturerId);

    @Query("select count(m) from Manufacturer m where m.typeEntryId = :entryId or m.subtypeEntryId = :entryId")
    long countReferencing(@Param("entryId") Long entryId);
}
