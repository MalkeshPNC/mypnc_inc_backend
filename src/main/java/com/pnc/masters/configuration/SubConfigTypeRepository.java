package com.pnc.masters.configuration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubConfigTypeRepository extends JpaRepository<SubConfigType, Long> {

    boolean existsByTypeCodeIgnoreCase(String typeCode);

    Optional<SubConfigType> findByTypeCodeIgnoreCase(String typeCode);

    List<SubConfigType> findAllByOrderByTypeNameAsc();

    @Query("""
            SELECT t FROM SubConfigType t
            WHERE t.moduleCode IS NULL OR UPPER(t.moduleCode) = UPPER(:moduleCode)
            ORDER BY t.typeName ASC
            """)
    List<SubConfigType> findSharedOrModule(@Param("moduleCode") String moduleCode);
}
