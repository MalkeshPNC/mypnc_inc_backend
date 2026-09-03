package com.pnc.masters.ncmaster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NcMasterRepository extends JpaRepository<NcMaster, Long> {

    boolean existsByNcNumberIgnoreCase(String ncNumber);

    boolean existsByNcNumberIgnoreCaseAndNcIdNot(String ncNumber, Long ncId);

    List<NcMaster> findAllByOrderByCreatedAtDesc();
}
