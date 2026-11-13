package com.pnc.masters.party;

import java.util.Collection;
import java.util.Map;

public interface PartyLookup {

    PartyChoice resolve(PartyList list, Long typeEntryId, Long subtypeEntryId);

    Map<Long, String> names(Collection<Long> entryIds);
}
