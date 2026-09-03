package com.pnc.masters.ncmaster;

import com.pnc.masters.ncmaster.api.NcMasterRequest;
import com.pnc.masters.ncmaster.api.NcMasterResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/nc-masters")
public class NcMasterController {

    private final NcMasterService ncMasterService;

    public NcMasterController(NcMasterService ncMasterService) {
        this.ncMasterService = ncMasterService;
    }

    @GetMapping
    public List<NcMasterResponse> findAll() {
        return ncMasterService.findAll();
    }

    @PostMapping
    public ResponseEntity<NcMasterResponse> create(
            @Valid @RequestBody NcMasterRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        NcMasterResponse response = ncMasterService.create(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.ncId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public NcMasterResponse update(@PathVariable Long id, @Valid @RequestBody NcMasterRequest request) {
        return ncMasterService.update(id, request);
    }
}
