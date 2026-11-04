package com.pnc.masters.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findAllByOrderByEmailAsc();

    List<AppUser> findByEnabledTrueOrderByDisplayNameAsc();

    @Query("""
            SELECT DISTINCT u FROM AppUser u
            JOIN u.roles r
            WHERE u.enabled = true
              AND UPPER(r.roleCode) = UPPER(:roleCode)
            ORDER BY u.displayName ASC
            """)
    List<AppUser> findEnabledByRoleCode(@Param("roleCode") String roleCode);

    @Query("""
            SELECT DISTINCT u FROM AppUser u
            JOIN u.roles r
            WHERE u.enabled = true
              AND (
                r.roleCode = 'SUPER_ADMIN'
                OR r.roleId IN (
                    SELECT a.id.roleId FROM RoleModuleAccess a, AppModule m
                    WHERE m.moduleId = a.id.moduleId
                      AND m.moduleCode = :moduleCode
                      AND a.accessLevel <> com.pnc.masters.security.ModuleAccessLevel.NONE
                )
              )
            ORDER BY u.displayName ASC
            """)
    List<AppUser> findEnabledWithModuleAccess(@Param("moduleCode") String moduleCode);

    List<AppUser> findByEnabledTrueAndUserIdNotOrderByDisplayNameAsc(Long userId);

    @Query("""
            SELECT COUNT(u) FROM AppUser u
            JOIN u.roles r
            WHERE r.roleCode = 'ADMIN' AND u.enabled = true
            """)
    long countEnabledAdmins();
}
