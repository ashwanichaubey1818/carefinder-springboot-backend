package com.carefinder.backend.security;

import com.carefinder.backend.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null
                && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(authorization.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            JwtPrincipal principal = jwtService.parse(token);
            userRepository.findById(principal.userId())
                    .filter(user -> user.isEnabled() && user.getTokenVersion() == principal.tokenVersion())
                    .ifPresent(user -> {
                        JwtPrincipal verifiedPrincipal = new JwtPrincipal(
                                user.getId(),
                                user.getEmail(),
                                user.getRole(),
                                user.getTokenVersion()
                        );
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        verifiedPrincipal,
                                        token,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        } catch (IllegalArgumentException exception) {
            log.debug("Rejected bearer token: {}", exception.getMessage());
        }
    }
}
