package com.pnc.masters.security;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "tblrole_module_access")
public class RoleModuleAccess {

    @EmbeddedId
    private RoleModuleAccessId id = new RoleModuleAccessId();

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 10)
    private ModuleAccessLevel accessLevel;

    public RoleModuleAccessId getId() { return id; }
    public void setId(RoleModuleAccessId id) { this.id = id; }
    public ModuleAccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(ModuleAccessLevel accessLevel) { this.accessLevel = accessLevel; }
}
