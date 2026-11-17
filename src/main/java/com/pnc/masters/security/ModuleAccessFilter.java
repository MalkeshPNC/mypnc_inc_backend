package com.pnc.masters.security;

import com.pnc.masters.security.ModuleAccessPolicy.AccessDecision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ModuleAccessFilter extends OncePerRequestFilter {

    private final ModuleAccessService moduleAccessService;

    public ModuleAccessFilter(ModuleAccessService moduleAccessService) {
        this.moduleAccessService = moduleAccessService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        AccessDecision decision = ModuleAccessPolicy.decide(request.getMethod(), path);
        if (decision.kind() == AccessDecision.Kind.OPEN) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!moduleAccessService.allows(userId, decision)) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Forbidden\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
