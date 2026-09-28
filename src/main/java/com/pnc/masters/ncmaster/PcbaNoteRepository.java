package com.pnc.masters.ncmaster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PcbaNoteRepository extends JpaRepository<PcbaNote, Long> {

    Optional<PcbaNote> findByPcbaPartNumberIgnoreCase(String pcbaPartNumber);
}
