package com.pnc.masters.vendor;

import com.pnc.masters.party.PartyList;
import com.pnc.masters.party.PartyLookup;
import com.pnc.masters.party.PartyService;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
public class VendorService extends PartyService<Vendor> {

    private final VendorRepository vendors;

    public VendorService(VendorRepository vendors, PartyLookup catalog, AppUserRepository users, Clock clock) {
        super(catalog, users, clock);
        this.vendors = vendors;
    }

    @Override
    protected PartyList listKind() {
        return PartyList.VENDOR;
    }

    @Override
    protected String missingMessage() {
        return "Vendor was not found.";
    }

    @Override
    protected Vendor newRow() {
        return new Vendor();
    }

    @Override
    protected List<Vendor> findActive() {
        return vendors.findByDeletedFalseOrderByCreatedDateDescVendorIdDesc();
    }

    @Override
    protected Optional<Vendor> findRow(Long id) {
        return vendors.findById(id);
    }

    @Override
    protected boolean codeTaken(String code, Long excludeId) {
        return excludeId == null
                ? vendors.existsByCodeIgnoreCase(code)
                : vendors.existsByCodeIgnoreCaseAndVendorIdNot(code, excludeId);
    }

    @Override
    protected Vendor save(Vendor row) {
        return vendors.save(row);
    }
}
