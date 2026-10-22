package com.pnc.masters.quote.application;

/** Current-year win ratio for one customer, using the same rule as customer quote history. */
public interface CustomerWinRatio {

    int currentYear(Long custId, String customerName);
}
