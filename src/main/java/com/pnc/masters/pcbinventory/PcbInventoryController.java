package com.pnc.masters.pcbinventory;

import com.pnc.masters.pcbinventory.api.PcbInventoryDetailResponse;
import com.pnc.masters.pcbinventory.api.PcbInventoryItemResponse;
import com.pnc.masters.pcbinventory.api.PcbInventoryLookupsResponse;
import com.pnc.masters.pcbinventory.api.PcbInventoryReceiptRequest;
import com.pnc.masters.pcbinventory.api.PcbInventoryRequest;
import com.pnc.masters.pcbinventory.api.PcbInventoryTransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pcb-inventory")
public class PcbInventoryController {

    private final PcbInventoryService pcbInventoryService;

    public PcbInventoryController(PcbInventoryService pcbInventoryService) {
        this.pcbInventoryService = pcbInventoryService;
    }

    @GetMapping
    public List<PcbInventoryItemResponse> list(@RequestParam(defaultValue = "all") String view) {
        return pcbInventoryService.list(view);
    }

    @GetMapping("/lookups")
    public PcbInventoryLookupsResponse lookups() {
        return pcbInventoryService.lookups();
    }

    @GetMapping("/{id}")
    public PcbInventoryDetailResponse findById(@PathVariable Long id) {
        return pcbInventoryService.findById(id);
    }

    @PostMapping
    public ResponseEntity<PcbInventoryItemResponse> create(@Valid @RequestBody PcbInventoryRequest request) {
        PcbInventoryItemResponse response = pcbInventoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.itemId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public PcbInventoryItemResponse update(@PathVariable Long id, @Valid @RequestBody PcbInventoryRequest request) {
        return pcbInventoryService.update(id, request);
    }

    @PostMapping("/{id}/transactions")
    public PcbInventoryItemResponse transact(@PathVariable Long id,
                                              @Valid @RequestBody PcbInventoryTransactionRequest request) {
        return pcbInventoryService.transact(id, request);
    }

    @PostMapping("/{id}/receipts")
    public PcbInventoryItemResponse receive(@PathVariable Long id,
                                             @Valid @RequestBody PcbInventoryReceiptRequest request,
                                             Authentication authentication) {
        return pcbInventoryService.receive(id, request, userId(authentication));
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
