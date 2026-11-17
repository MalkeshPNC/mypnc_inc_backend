package com.pnc.masters.manufacturer;

import com.pnc.masters.manufacturer.api.ManufacturerRequest;
import com.pnc.masters.party.api.PartyResponse;
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
@RequestMapping("/api/v1/manufacturers")
public class ManufacturerController {

    private final ManufacturerService manufacturers;

    public ManufacturerController(ManufacturerService manufacturers) {
        this.manufacturers = manufacturers;
    }

    @GetMapping
    public List<PartyResponse> list() {
        return manufacturers.list();
    }

    @GetMapping("/{id}")
    public PartyResponse findById(@PathVariable Long id) {
        return manufacturers.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartyResponse create(@RequestBody ManufacturerRequest request, Authentication authentication) {
        return manufacturers.create(userId(authentication), request);
    }

    @PutMapping("/{id}")
    public PartyResponse update(
            @PathVariable Long id,
            @RequestBody ManufacturerRequest request,
            Authentication authentication
    ) {
        return manufacturers.update(id, userId(authentication), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        manufacturers.delete(id);
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
