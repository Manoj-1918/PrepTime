package com.preptime.web;

import com.preptime.repo.TokenRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Very small token check: every /api call except login/register needs "Authorization: Bearer <token>". */
@Component
public class AuthFilter extends OncePerRequestFilter {
    private final TokenRepo tokens;

    public AuthFilter(TokenRepo tokens) { this.tokens = tokens; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        String p = r.getRequestURI();
        return !p.startsWith("/api/") || p.equals("/api/auth/login") || p.equals("/api/auth/register")
                || p.equals("/api/health") || "OPTIONS".equals(r.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            var t = tokens.findById(h.substring(7));
            if (t.isPresent()) {
                req.setAttribute("userId", t.get().userId);
                chain.doFilter(req, res);
                return;
            }
        }
        res.setStatus(401);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"Please log in\"}");
    }
}
