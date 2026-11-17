package com.pnc.masters.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoleModuleAccessRepository extends JpaRepository<RoleModuleAccess, RoleModuleAccessId> {

    List<RoleModuleAccess> findByIdRoleId(Long roleId);

    List<RoleModuleAccess> findByIdRoleIdIn(Collection<Long> roleIds);

    void deleteByIdRoleId(Long roleId);
}
