package com.pnc.masters.security;

import com.pnc.masters.security.api.ModuleConflictException;
import com.pnc.masters.security.api.ModuleNotFoundException;
import com.pnc.masters.security.api.ModulePermissionResponse;
import com.pnc.masters.security.api.ModuleRequest;
import com.pnc.masters.security.api.ModuleResponse;
import com.pnc.masters.security.api.PermissionResponse;
import com.pnc.masters.security.api.RoleAccessResponse;
import com.pnc.masters.security.api.RoleAccessUpdateRequest;
import com.pnc.masters.security.api.RoleNotFoundException;
import com.pnc.masters.security.api.UserNotFoundException;
import com.pnc.masters.security.ModuleAccessPolicy.AccessDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ModuleAccessService {

    private final AppUserRepository userRepository;
    private final AppModuleRepository moduleRepository;
    private final RoleModuleAccessRepository accessRepository;
    private final RoleRepository roleRepository;

    public ModuleAccessService(
            AppUserRepository userRepository,
            AppModuleRepository moduleRepository,
            RoleModuleAccessRepository accessRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
        this.accessRepository = accessRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public boolean allows(Long userId, AccessDecision decision) {
        if (decision.kind() == AccessDecision.Kind.OPEN) {
            return true;
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEnabled()) {
            return false;
        }
        if (isSuperAdmin(user)) {
            return true;
        }
        if (decision.kind() == AccessDecision.Kind.SUPER_ADMIN) {
            return false;
        }
        Map<String, ModuleAccessLevel> merged = merge(user);
        if (decision.kind() == AccessDecision.Kind.MODULE) {
            return merged.getOrDefault(decision.modules().getFirst(), ModuleAccessLevel.NONE).allows(decision.level());
        }
        return decision.modules().stream()
                .anyMatch(code -> merged.getOrDefault(code, ModuleAccessLevel.NONE).allows(decision.level()));
    }

    @Transactional(readOnly = true)
    public PermissionResponse permissionsFor(Long userId) {
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        boolean superAdmin = isSuperAdmin(user);
        Map<String, ModuleAccessLevel> merged = superAdmin ? Map.of() : merge(user);
        List<ModulePermissionResponse> modules = moduleRepository.findAllByOrderByModuleIdAsc().stream()
                .map(module -> new ModulePermissionResponse(
                        module.getModuleCode(),
                        module.getModuleName(),
                        superAdmin ? ModuleAccessLevel.WRITE : merged.getOrDefault(module.getModuleCode(), ModuleAccessLevel.NONE)
                ))
                .toList();
        return new PermissionResponse(superAdmin, modules);
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> findModules() {
        return moduleRepository.findAllByOrderByModuleIdAsc().stream().map(this::toResponse).toList();
    }

    public ModuleResponse createModule(ModuleRequest request) {
        String code = request.moduleCode().trim().toUpperCase(Locale.ROOT);
        if (moduleRepository.existsByModuleCodeIgnoreCase(code)) {
            throw new ModuleConflictException("Module " + code + " already exists");
        }
        AppModule module = new AppModule();
        module.setModuleCode(code);
        module.setModuleName(request.moduleName().trim());
        return toResponse(moduleRepository.save(module));
    }

    @Transactional(readOnly = true)
    public List<RoleAccessResponse> accessFor(Long roleId) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
        Map<Long, ModuleAccessLevel> stored = accessRepository.findByIdRoleId(role.getRoleId()).stream()
                .collect(Collectors.toMap(row -> row.getId().getModuleId(), RoleModuleAccess::getAccessLevel));
        return moduleRepository.findAllByOrderByModuleIdAsc().stream()
                .map(module -> new RoleAccessResponse(
                        module.getModuleId(),
                        module.getModuleCode(),
                        module.getModuleName(),
                        stored.getOrDefault(module.getModuleId(), ModuleAccessLevel.NONE)
                ))
                .toList();
    }

    public List<RoleAccessResponse> replaceAccess(Long roleId, RoleAccessUpdateRequest request) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
        List<AppModule> modules = moduleRepository.findAllByOrderByModuleIdAsc();
        Map<Long, ModuleAccessLevel> requested = new HashMap<>();
        for (var grant : request.grants()) {
            if (requested.put(grant.moduleId(), grant.access()) != null) {
                throw new ModuleConflictException("Each module can be set once");
            }
        }
        Set<Long> moduleIds = modules.stream().map(AppModule::getModuleId).collect(Collectors.toSet());
        if (!requested.keySet().equals(moduleIds)) {
            throw new ModuleConflictException("Choose None, Read, or Write for every module");
        }
        if (AuthService.ROLE_SUPER_ADMIN.equals(role.getRoleCode())
                && requested.values().stream().anyMatch(level -> level != ModuleAccessLevel.WRITE)) {
            throw new ModuleConflictException("Super Admin keeps Write on every module");
        }
        accessRepository.deleteByIdRoleId(roleId);
        accessRepository.flush();
        for (AppModule module : modules) {
            RoleModuleAccess row = new RoleModuleAccess();
            row.setId(new RoleModuleAccessId(roleId, module.getModuleId()));
            row.setAccessLevel(requested.get(module.getModuleId()));
            accessRepository.save(row);
        }
        return modules.stream()
                .map(module -> new RoleAccessResponse(
                        module.getModuleId(),
                        module.getModuleCode(),
                        module.getModuleName(),
                        requested.get(module.getModuleId())
                ))
                .toList();
    }

    private Map<String, ModuleAccessLevel> merge(AppUser user) {
        List<Long> roleIds = user.getRoles().stream().map(Role::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> codes = moduleRepository.findAllByOrderByModuleIdAsc().stream()
                .collect(Collectors.toMap(AppModule::getModuleId, AppModule::getModuleCode));
        Map<String, ModuleAccessLevel> merged = new HashMap<>();
        for (RoleModuleAccess row : accessRepository.findByIdRoleIdIn(roleIds)) {
            String code = codes.get(row.getId().getModuleId());
            if (code == null) {
                continue;
            }
            merged.merge(code, row.getAccessLevel(), ModuleAccessLevel::higher);
        }
        return merged;
    }

    private boolean isSuperAdmin(AppUser user) {
        return user.getRoles().stream().anyMatch(role -> AuthService.ROLE_SUPER_ADMIN.equals(role.getRoleCode()));
    }

    private ModuleResponse toResponse(AppModule module) {
        return new ModuleResponse(module.getModuleId(), module.getModuleCode(), module.getModuleName());
    }
}
