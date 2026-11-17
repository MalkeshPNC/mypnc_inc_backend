package com.pnc.masters.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeGroupRepository extends JpaRepository<EmployeeGroup, Long> {

    List<EmployeeGroup> findByDeletedFalseOrderByCreatedAtDescGroupIdDesc();

    List<EmployeeGroup> findByDeletedFalseAndTypeIgnoreCaseOrderByNameAsc(String type);

    List<EmployeeGroup> findByGroupIdInAndDeletedFalse(Collection<Long> groupIds);

    Optional<EmployeeGroup> findFirstByNameIgnoreCaseAndDeletedFalse(String name);
}
