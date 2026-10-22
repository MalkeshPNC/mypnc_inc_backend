package com.pnc.masters.security;

import com.pnc.masters.security.ModuleAccessPolicy.AccessDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleAccessPolicyTest {

    @Test
    void writeBeatsReadAndReadBeatsNone() {
        assertEquals(ModuleAccessLevel.WRITE, ModuleAccessLevel.higher(ModuleAccessLevel.READ, ModuleAccessLevel.WRITE));
        assertEquals(ModuleAccessLevel.READ, ModuleAccessLevel.higher(ModuleAccessLevel.NONE, ModuleAccessLevel.READ));
        assertTrue(ModuleAccessLevel.WRITE.allows(ModuleAccessLevel.READ));
        assertTrue(!ModuleAccessLevel.READ.allows(ModuleAccessLevel.WRITE));
    }

    @Test
    void quoteWritesNeedWriteAndReadsNeedRead() {
        assertEquals(module("QUOTE", ModuleAccessLevel.READ), ModuleAccessPolicy.decide("GET", "/api/v1/quotes"));
        assertEquals(module("QUOTE", ModuleAccessLevel.WRITE), ModuleAccessPolicy.decide("DELETE", "/api/v1/quotes/4"));
        assertEquals(module("QUOTE", ModuleAccessLevel.WRITE), ModuleAccessPolicy.decide("POST", "/api/v1/quotes/4/lock/takeover"));
        assertEquals(module("REPORTS", ModuleAccessLevel.READ), ModuleAccessPolicy.decide("GET", "/api/v1/quotes/report"));
        AccessDecision referred = ModuleAccessPolicy.decide("GET", "/api/v1/quotes/referred-summary");
        assertEquals(AccessDecision.Kind.ANY_MODULE, referred.kind());
        assertEquals(ModuleAccessLevel.READ, referred.level());
    }

    @Test
    void configurationBootstrapStaysOpenAndChangesNeedWrite() {
        assertEquals(AccessDecision.open(), ModuleAccessPolicy.decide("GET", "/api/v1/configurations"));
        assertEquals(module("CONFIGURATION", ModuleAccessLevel.WRITE), ModuleAccessPolicy.decide("PUT", "/api/v1/configurations/fai"));
    }

    @Test
    void roleChangesAndModulesAreSuperAdminOnly() {
        assertEquals(AccessDecision.superAdmin(), ModuleAccessPolicy.decide("POST", "/api/v1/roles"));
        assertEquals(AccessDecision.superAdmin(), ModuleAccessPolicy.decide("PUT", "/api/v1/roles/3/access"));
        assertEquals(AccessDecision.superAdmin(), ModuleAccessPolicy.decide("POST", "/api/v1/modules"));
        assertEquals(AccessDecision.superAdmin(), ModuleAccessPolicy.decide("GET", "/api/v1/quotes/customer-summary"));
        assertEquals(module("SECURITY", ModuleAccessLevel.READ), ModuleAccessPolicy.decide("GET", "/api/v1/roles"));
        assertEquals(module("SECURITY", ModuleAccessLevel.WRITE), ModuleAccessPolicy.decide("PUT", "/api/v1/users/2"));
    }

    @Test
    void quoteReadersCanLoadCustomerLookupsButNotChangeThem() {
        AccessDecision lookup = ModuleAccessPolicy.decide("GET", "/api/v1/customers");
        assertEquals(AccessDecision.Kind.ANY_MODULE, lookup.kind());
        assertEquals(ModuleAccessLevel.READ, lookup.level());
        assertEquals(module("CUSTOMERS", ModuleAccessLevel.WRITE), ModuleAccessPolicy.decide("POST", "/api/v1/customers"));
        assertEquals(AccessDecision.open(), ModuleAccessPolicy.decide("GET", "/api/v1/documents/10/content"));
    }

    private static AccessDecision module(String code, ModuleAccessLevel level) {
        return AccessDecision.module(code, level);
    }
}
