package com.pnc.masters.chat;

import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import com.pnc.masters.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class ChatStompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final AppUserRepository users;

    public ChatStompAuthChannelInterceptor(JwtService jwtService, AppUserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }
        String raw = firstHeader(accessor, "Authorization");
        if (raw == null) {
            raw = firstHeader(accessor, "token");
        }
        String token = stripBearer(raw);
        if (token.isBlank()) {
            throw new IllegalArgumentException("Chat socket requires a token");
        }
        try {
            Claims claims = jwtService.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            AppUser user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Unknown user"));
            if (!user.isEnabled()) {
                throw new IllegalArgumentException("User is disabled");
            }
            accessor.setUser(new ChatPrincipal(userId));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Chat socket token is not valid", exception);
        }
        return message;
    }

    private static String firstHeader(StompHeaderAccessor accessor, String name) {
        String value = accessor.getFirstNativeHeader(name);
        return value == null ? null : value.trim();
    }

    private static String stripBearer(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return raw.substring(7).trim();
        }
        return raw;
    }
}
