package com.pnc.masters.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findAllByOrderByEmailAsc();

    @Query("""
            SELECT COUNT(u) FROM AppUser u
            JOIN u.roles r
            WHERE r.roleCode = 'ADMIN' AND u.enabled = true
            """)
    long countEnabledAdmins();
}
