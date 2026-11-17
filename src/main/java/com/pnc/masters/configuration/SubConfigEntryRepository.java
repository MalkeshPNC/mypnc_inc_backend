package com.pnc.masters.configuration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubConfigEntryRepository extends JpaRepository<SubConfigEntry, Long> {

    List<SubConfigEntry> findAllByOrderByCreatedAtDesc();

    List<SubConfigEntry> findByTypeTypeIdOrderByCreatedAtDesc(Long typeId);
}
