package com.pnc.masters.customer.reminder;

import java.util.Set;

/** Which customers belong in a no-lead or announcement run. */
public interface ReminderAudienceLookup {

    Set<Long> customerIds(String kind);
}
