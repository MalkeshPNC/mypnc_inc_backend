package com.pnc.masters.quote.api;

import com.pnc.masters.quote.application.QuoteLockService;
import com.pnc.masters.quote.application.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/quotes")
public class QuoteController {

    private final QuoteService quoteService;
    private final QuoteLockService lockService;

    public QuoteController(QuoteService quoteService, QuoteLockService lockService) {
        this.quoteService = quoteService;
        this.lockService = lockService;
    }

    @GetMapping
    public List<QuoteSummaryResponse> findAll(Authentication authentication) {
        return quoteService.findAll(userId(authentication));
    }

    @GetMapping("/{id}/next-copy-number")
    public QuoteCopyNumberResponse nextCopyNumber(@PathVariable Long id) {
        return quoteService.nextCopyNumber(id);
    }

    @GetMapping("/{id}/family")
    public QuoteFamilyResponse findFamily(@PathVariable Long id, Authentication authentication) {
        return quoteService.findFamily(id, userId(authentication));
    }

    @GetMapping("/{id}")
    public QuoteResponse findById(@PathVariable Long id, Authentication authentication) {
        return quoteService.findById(id, userId(authentication));
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> create(
            @Valid @RequestBody QuoteRequest request,
            Authentication authentication
    ) {
        QuoteResponse response = quoteService.create(request, userId(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.qid()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public QuoteResponse update(
            @PathVariable Long id,
            @Valid @RequestBody QuoteRequest request,
            Authentication authentication
    ) {
        return quoteService.update(id, request, userId(authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Acquires the edit lock, and doubles as the heartbeat that keeps it alive. */
    @PostMapping("/{id}/lock")
    public QuoteLockResponse lock(@PathVariable Long id, Authentication authentication) {
        return lockService.acquireOrRenew(id, userId(authentication));
    }

    @DeleteMapping("/{id}/lock")
    public ResponseEntity<Void> releaseLock(@PathVariable Long id, Authentication authentication) {
        lockService.release(id, userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/lock/force")
    public ResponseEntity<Void> forceReleaseLock(@PathVariable Long id) {
        lockService.forceRelease(id);
        return ResponseEntity.noContent().build();
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
