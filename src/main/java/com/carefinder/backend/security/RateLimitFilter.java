package com.carefinder.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        int limit = requestLimit(request);
        if (limit == 0) {
            filterChain.doFilter(request, response);
            return;
        }
        long now = System.currentTimeMillis();
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= WINDOW_MILLIS);
        }
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || now - current.startedAt() >= WINDOW_MILLIS
                        ? new Window(now, new AtomicInteger(1))
                        : increment(current)
        );
        if (window.requests().get() > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", 429,
                    "message", "Too many requests. Please try again in one minute.",
                    "path", request.getRequestURI()
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Window increment(Window window) {
        window.requests().incrementAndGet();
        return window;
    }

    private int requestLimit(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return 0;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/chatbot/")) {
            return 60;
        }
        if (path.startsWith("/api/v1/auth/")) {
            return 12;
        }
        return 0;
    }

    private record Window(long startedAt, AtomicInteger requests) {
    }
}
