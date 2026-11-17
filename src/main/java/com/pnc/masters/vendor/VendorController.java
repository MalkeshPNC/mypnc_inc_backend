package com.pnc.masters.vendor;

import com.pnc.masters.party.api.PartyResponse;
import com.pnc.masters.vendor.api.VendorRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendors;

    public VendorController(VendorService vendors) {
        this.vendors = vendors;
    }

    @GetMapping
    public List<PartyResponse> list() {
        return vendors.list();
    }

    @GetMapping("/{id}")
    public PartyResponse findById(@PathVariable Long id) {
        return vendors.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartyResponse create(@RequestBody VendorRequest request, Authentication authentication) {
        return vendors.create(userId(authentication), request);
    }

    @PutMapping("/{id}")
    public PartyResponse update(@PathVariable Long id, @RequestBody VendorRequest request, Authentication authentication) {
        return vendors.update(id, userId(authentication), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        vendors.delete(id);
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
