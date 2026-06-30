package com.teleconnect.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.renewal.threshold.ms}")
    private long renewalThresholdMs;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                List<String> perms = jwtUtil.extractPermissions(token);
                List<SimpleGrantedAuthority> authorities = perms.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Threshold-based sliding expiry: if less than the threshold
                // remains, issue a fresh token in the response Authorization header.
                long remainingMs = jwtUtil.getExpiryMs(token) - System.currentTimeMillis();
                if (remainingMs > 0 && remainingMs < renewalThresholdMs) {
                    String newToken = jwtUtil.generateToken(email, perms);
                    res.setHeader("Authorization", "Bearer " + newToken);
                }
            }
        }
        chain.doFilter(req, res);
    }
}
