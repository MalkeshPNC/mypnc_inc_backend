package com.pnc.masters.security.api;

public record ModuleResponse(
        Long moduleId,
        String moduleCode,
        String moduleName
) {
}
