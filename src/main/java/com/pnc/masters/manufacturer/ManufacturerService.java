package com.pnc.masters.manufacturer;

import com.pnc.masters.party.PartyList;
import com.pnc.masters.party.PartyLookup;
import com.pnc.masters.party.PartyService;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
public class ManufacturerService extends PartyService<Manufacturer> {

    private final ManufacturerRepository manufacturers;

    public ManufacturerService(
            ManufacturerRepository manufacturers,
            PartyLookup catalog,
            AppUserRepository users,
            Clock clock
    ) {
        super(catalog, users, clock);
        this.manufacturers = manufacturers;
    }

    @Override
    protected PartyList listKind() {
        return PartyList.MANUFACTURER;
    }

    @Override
    protected String missingMessage() {
        return "Manufacturer was not found.";
    }

    @Override
    protected Manufacturer newRow() {
        return new Manufacturer();
    }

    @Override
    protected List<Manufacturer> findActive() {
        return manufacturers.findByDeletedFalseOrderByCreatedDateDescManufacturerIdDesc();
    }

    @Override
    protected Optional<Manufacturer> findRow(Long id) {
        return manufacturers.findById(id);
    }

    @Override
    protected boolean codeTaken(String code, Long excludeId) {
        return excludeId == null
                ? manufacturers.existsByCodeIgnoreCase(code)
                : manufacturers.existsByCodeIgnoreCaseAndManufacturerIdNot(code, excludeId);
    }

    @Override
    protected Manufacturer save(Manufacturer row) {
        return manufacturers.save(row);
    }
}
