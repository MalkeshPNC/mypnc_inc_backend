package com.pnc.masters.salesperson;

import com.pnc.masters.salesperson.api.SalesPersonRequest;
import com.pnc.masters.salesperson.api.SalesPersonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/salespersons")
public class SalesPersonController {

    private final SalesPersonService salesPersonService;

    public SalesPersonController(SalesPersonService salesPersonService) {
        this.salesPersonService = salesPersonService;
    }

    @GetMapping
    public List<SalesPersonResponse> findAll() { return salesPersonService.findAll(); }

    @GetMapping("/{id}")
    public SalesPersonResponse findById(@PathVariable Long id) { return salesPersonService.findById(id); }

    @PostMapping
    public ResponseEntity<SalesPersonResponse> create(@Valid @RequestBody SalesPersonRequest request) {
        SalesPersonResponse response = salesPersonService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.spId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public SalesPersonResponse update(@PathVariable Long id, @Valid @RequestBody SalesPersonRequest request) {
        return salesPersonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesPersonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}