package com.pnc.masters.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EmployeeGroupMemberRepository extends JpaRepository<EmployeeGroupMember, EmployeeGroupMember.Key> {

    List<EmployeeGroupMember> findByGroupIdIn(Collection<Long> groupIds);

    List<EmployeeGroupMember> findByUserId(Long userId);

    List<EmployeeGroupMember> findByUserIdIn(Collection<Long> userIds);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupId(Long groupId);

    void deleteByUserId(Long userId);
}
