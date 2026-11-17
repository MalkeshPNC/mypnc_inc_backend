package com.pnc.masters.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppModuleRepository extends JpaRepository<AppModule, Long> {

    boolean existsByModuleCodeIgnoreCase(String moduleCode);

    List<AppModule> findAllByOrderByModuleIdAsc();
}
