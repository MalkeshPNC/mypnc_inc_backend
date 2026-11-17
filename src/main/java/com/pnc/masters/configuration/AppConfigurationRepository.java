package com.pnc.masters.configuration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, String> {

    List<AppConfiguration> findAllByOrderByConfigKeyAsc();
}
