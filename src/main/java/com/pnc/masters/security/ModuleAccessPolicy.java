package com.pnc.masters.security;

import java.util.List;

/**
 * Maps an API call to the module access it needs. Paths that are not a module
 * stay open for any signed-in user.
 */
public final class ModuleAccessPolicy {

    private ModuleAccessPolicy() {
    }

    public static AccessDecision decide(String method, String path) {
        String http = method == null ? "" : method.toUpperCase();
        String uri = normalize(path);
        if (uri.startsWith("/api/v1/modules") || uri.matches("/api/v1/roles/\\d+/access")) {
            return AccessDecision.superAdmin();
        }
        if (uri.startsWith("/api/v1/roles")) {
            return isWrite(http) ? AccessDecision.superAdmin() : AccessDecision.module("SECURITY", ModuleAccessLevel.READ);
        }
        if (uri.startsWith("/api/v1/users")) {
            return AccessDecision.module("SECURITY", isWrite(http) ? ModuleAccessLevel.WRITE : ModuleAccessLevel.READ);
        }
        if ("/api/v1/configurations".equals(uri) && !isWrite(http)) {
            return AccessDecision.open();
        }
        if (uri.startsWith("/api/v1/configurations")) {
            return AccessDecision.module("CONFIGURATION", isWrite(http) ? ModuleAccessLevel.WRITE : ModuleAccessLevel.READ);
        }
        if ("/api/v1/quotes/customer-summary".equals(uri)) {
            return AccessDecision.superAdmin();
        }
        if ("/api/v1/quotes/report".equals(uri) || uri.startsWith("/api/v1/quotes/report/")) {
            return AccessDecision.module("REPORTS", ModuleAccessLevel.READ);
        }
        if ("/api/v1/quotes/referred-summary".equals(uri)) {
            return AccessDecision.any(ModuleAccessLevel.READ, "QUOTE", "REPORTS");
        }
        if (uri.startsWith("/api/v1/quotes") || uri.startsWith("/api/v1/nc-masters")) {
            return AccessDecision.module("QUOTE", isWrite(http) ? ModuleAccessLevel.WRITE : ModuleAccessLevel.READ);
        }
        if (uri.startsWith("/api/v1/customers") || uri.startsWith("/api/v1/contacts") || uri.startsWith("/api/v1/salespersons")) {
            if (!isWrite(http)) {
                return AccessDecision.any(ModuleAccessLevel.READ, "QUOTE", "CUSTOMERS");
            }
            return AccessDecision.module("CUSTOMERS", ModuleAccessLevel.WRITE);
        }
        if (uri.startsWith("/api/v1/documents")) {
            if (!isWrite(http)) {
                return AccessDecision.open();
            }
            return AccessDecision.any(ModuleAccessLevel.WRITE, "QUOTE", "CUSTOMERS", "CONFIGURATION");
        }
        if (uri.matches("/api/v1/tasks/\\d+/progress")) {
            return AccessDecision.module("TASKS", ModuleAccessLevel.READ);
        }
        if (uri.startsWith("/api/v1/tasks")) {
            return AccessDecision.module("TASKS", isWrite(http) ? ModuleAccessLevel.WRITE : ModuleAccessLevel.READ);
        }
        if (uri.startsWith("/api/v1/development-track")) {
            return AccessDecision.module("DEVELOPMENT_TRACK", isWrite(http) ? ModuleAccessLevel.WRITE : ModuleAccessLevel.READ);
        }
        if (uri.startsWith("/api/v1/employee-groups")) {
            return AccessDecision.module("SECURITY", isWrite(http) ? ModuleAccessLevel.WRITE : ModuleAccessLevel.READ);
        }
        return AccessDecision.open();
    }

    static boolean isWrite(String method) {
        return !"GET".equals(method) && !"HEAD".equals(method) && !"OPTIONS".equals(method);
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String uri = path;
        int query = uri.indexOf('?');
        if (query >= 0) {
            uri = uri.substring(0, query);
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    public record AccessDecision(Kind kind, ModuleAccessLevel level, List<String> modules) {
        public enum Kind { OPEN, SUPER_ADMIN, MODULE, ANY_MODULE }

        public static AccessDecision open() {
            return new AccessDecision(Kind.OPEN, ModuleAccessLevel.NONE, List.of());
        }

        public static AccessDecision superAdmin() {
            return new AccessDecision(Kind.SUPER_ADMIN, ModuleAccessLevel.WRITE, List.of());
        }

        public static AccessDecision module(String code, ModuleAccessLevel level) {
            return new AccessDecision(Kind.MODULE, level, List.of(code));
        }

        public static AccessDecision any(ModuleAccessLevel level, String... codes) {
            return new AccessDecision(Kind.ANY_MODULE, level, List.of(codes));
        }
    }
}
