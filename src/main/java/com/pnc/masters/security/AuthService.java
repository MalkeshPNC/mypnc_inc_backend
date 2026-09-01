package com.pnc.masters.security;

import com.pnc.masters.security.api.AuthResponse;
import com.pnc.masters.security.api.AuthUserResponse;
import com.pnc.masters.security.api.DuplicateEmailException;
import com.pnc.masters.security.api.InvalidCredentialsException;
import com.pnc.masters.security.api.LoginRequest;
import com.pnc.masters.security.api.RoleNotFoundException;
import com.pnc.masters.security.api.SignupRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        Role userRole = roleRepository.findByRoleCodeIgnoreCase(ROLE_USER)
                .orElseThrow(() -> new RoleNotFoundException(ROLE_USER));
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setEnabled(true);
        user.getRoles().add(userRole);
        return toAuthResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(Long userId) {
        AppUser user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException();
        }
        return toUserResponse(user);
    }

    AuthResponse toAuthResponse(AppUser user) {
        AuthUserResponse profile = toUserResponse(user);
        String token = jwtService.createToken(user.getUserId(), user.getEmail(), profile.roles());
        return new AuthResponse(token, "Bearer", jwtService.expirationMs(), profile);
    }

    static AuthUserResponse toUserResponse(AppUser user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleCode)
                .sorted()
                .toList();
        return new AuthUserResponse(user.getUserId(), user.getEmail(), user.getDisplayName(), roles);
    }
}
