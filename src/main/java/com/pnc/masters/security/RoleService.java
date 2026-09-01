package com.pnc.masters.security;

import com.pnc.masters.security.api.RoleConflictException;
import com.pnc.masters.security.api.RoleNotFoundException;
import com.pnc.masters.security.api.RoleRequest;
import com.pnc.masters.security.api.RoleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final AppUserRepository userRepository;

    public RoleService(RoleRepository roleRepository, AppUserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAllByOrderByRoleCodeAsc().stream().map(this::toResponse).toList();
    }

    public RoleResponse create(RoleRequest request) {
        String code = request.roleCode().trim().toUpperCase(Locale.ROOT);
        if (roleRepository.existsByRoleCodeIgnoreCase(code)) {
            throw new RoleConflictException("Role " + code + " already exists");
        }
        Role role = new Role();
        role.setRoleCode(code);
        role.setRoleName(request.roleName().trim());
        role.setSystem(false);
        return toResponse(roleRepository.save(role));
    }

    public RoleResponse update(Long id, RoleRequest request) {
        Role role = getRole(id);
        String code = request.roleCode().trim().toUpperCase(Locale.ROOT);
        if (role.isSystem() && !role.getRoleCode().equalsIgnoreCase(code)) {
            throw new RoleConflictException("System role codes cannot be changed");
        }
        if (!role.getRoleCode().equalsIgnoreCase(code) && roleRepository.existsByRoleCodeIgnoreCase(code)) {
            throw new RoleConflictException("Role " + code + " already exists");
        }
        if (!role.isSystem()) {
            role.setRoleCode(code);
        }
        role.setRoleName(request.roleName().trim());
        return toResponse(roleRepository.save(role));
    }

    public void delete(Long id) {
        Role role = getRole(id);
        if (role.isSystem()) {
            throw new RoleConflictException("System roles cannot be deleted");
        }
        boolean assigned = userRepository.findAll().stream()
                .anyMatch(user -> user.getRoles().stream().anyMatch(item -> item.getRoleId().equals(id)));
        if (assigned) {
            throw new RoleConflictException("Role is still assigned to one or more users");
        }
        roleRepository.delete(role);
    }

    private Role getRole(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getRoleId(), role.getRoleCode(), role.getRoleName(), role.isSystem());
    }
}
