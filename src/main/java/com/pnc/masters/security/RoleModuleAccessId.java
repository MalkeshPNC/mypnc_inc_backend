package com.pnc.masters.security;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RoleModuleAccessId implements Serializable {

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "module_id")
    private Long moduleId;

    public RoleModuleAccessId() {
    }

    public RoleModuleAccessId(Long roleId, Long moduleId) {
        this.roleId = roleId;
        this.moduleId = moduleId;
    }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RoleModuleAccessId that)) {
            return false;
        }
        return Objects.equals(roleId, that.roleId) && Objects.equals(moduleId, that.moduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, moduleId);
    }
}
