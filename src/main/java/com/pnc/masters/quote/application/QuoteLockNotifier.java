package com.pnc.masters.quote.application;

import com.pnc.masters.quote.api.QuoteLockNotice;

/** Delivers takeover events to a signed-in user, and reports whether they have a live socket. */
public interface QuoteLockNotifier {

    boolean isConnected(Long userId);

    void send(Long userId, QuoteLockNotice notice);
}
