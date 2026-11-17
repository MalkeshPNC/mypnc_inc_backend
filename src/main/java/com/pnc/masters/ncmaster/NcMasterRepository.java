package com.pnc.masters.ncmaster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NcMasterRepository extends JpaRepository<NcMaster, Long> {

    Optional<NcMaster> findByNcNumberIgnoreCase(String ncNumber);

    boolean existsByNcNumberIgnoreCase(String ncNumber);

    boolean existsByNcNumberIgnoreCaseAndNcIdNot(String ncNumber, Long ncId);

    List<NcMaster> findAllByOrderByCreatedAtDesc();
}
