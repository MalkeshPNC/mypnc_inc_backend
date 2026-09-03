package com.pnc.masters.configuration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubConfigTypeRepository extends JpaRepository<SubConfigType, Long> {

    boolean existsByTypeCodeIgnoreCase(String typeCode);

    List<SubConfigType> findAllByOrderByTypeNameAsc();
}
