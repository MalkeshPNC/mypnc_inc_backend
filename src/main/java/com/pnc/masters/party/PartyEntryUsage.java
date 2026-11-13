package com.pnc.masters.party;

import com.pnc.masters.configuration.MasterEntryUsage;
import com.pnc.masters.manufacturer.ManufacturerRepository;
import com.pnc.masters.vendor.VendorRepository;
import org.springframework.stereotype.Component;

@Component
public class PartyEntryUsage implements MasterEntryUsage {

    private final VendorRepository vendors;
    private final ManufacturerRepository manufacturers;

    public PartyEntryUsage(VendorRepository vendors, ManufacturerRepository manufacturers) {
        this.vendors = vendors;
        this.manufacturers = manufacturers;
    }

    @Override
    public boolean isReferenced(Long entryId) {
        if (entryId == null) {
            return false;
        }
        return vendors.countReferencing(entryId) > 0 || manufacturers.countReferencing(entryId) > 0;
    }
}
