package com.pnc.masters.quote.application;

import com.pnc.masters.quote.api.QuoteLockNotice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

@Component
public class StompQuoteLockNotifier implements QuoteLockNotifier {

    static final String USER_QUEUE = "/queue/quote-locks";

    private final SimpMessagingTemplate broker;
    private final SimpUserRegistry sessions;

    public StompQuoteLockNotifier(SimpMessagingTemplate broker, SimpUserRegistry sessions) {
        this.broker = broker;
        this.sessions = sessions;
    }

    @Override
    public boolean isConnected(Long userId) {
        return sessions.getUser(String.valueOf(userId)) != null;
    }

    @Override
    public void send(Long userId, QuoteLockNotice notice) {
        broker.convertAndSendToUser(String.valueOf(userId), USER_QUEUE, notice);
    }
}
