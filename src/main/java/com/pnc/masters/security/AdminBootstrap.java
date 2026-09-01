package com.pnc.masters.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    public AdminBootstrap(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.countEnabledAdmins() > 0) {
            return;
        }
        Role adminRole = roleRepository.findByRoleCodeIgnoreCase(AuthService.ROLE_ADMIN).orElseThrow();
        String email = properties.getBootstrapEmail().trim().toLowerCase();
        AppUser admin = userRepository.findByEmailIgnoreCase(email).orElseGet(AppUser::new);
        admin.setEmail(email);
        admin.setDisplayName("Administrator");
        admin.setPasswordHash(passwordEncoder.encode(properties.getBootstrapPassword()));
        admin.setEnabled(true);
        admin.getRoles().add(adminRole);
        userRepository.save(admin);
        log.warn("Bootstrap admin created for {}. Change AUTH_BOOTSTRAP_PASSWORD after first login.", email);
    }
}
