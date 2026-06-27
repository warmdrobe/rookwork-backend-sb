package com.example.rookwork_backend_sb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP rate-limiting filter based on the Token Bucket algorithm (Bucket4j).
 *
 * <p>Applied <em>before</em> the JWT filter so that unauthenticated brute-force
 * attempts are blocked at the filter layer without touching any business logic.
 *
 * <p><b>Rate rules (per client IP):</b>
 * <ul>
 *   <li>{@code /api/auth/login}, {@code /api/auth/forgot-password},
 *       {@code /api/auth/reset-password} → <strong>5 requests / minute</strong></li>
 *   <li>{@code /api/invitations/**} → <strong>10 requests / minute</strong></li>
 * </ul>
 *
 * <p>All other paths are passed through without any limit.
 *
 * <p><b>Note:</b> The in-memory {@link ConcurrentHashMap} used here is suitable for
 * single-instance deployments. For horizontal scaling, replace the map with a
 * Redis-backed Bucket4j proxy store.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Max requests per minute for sensitive auth endpoints. */
    private static final int AUTH_LIMIT_PER_MINUTE = 5;

    /** Max requests per minute for invitation endpoints. */
    private static final int INVITE_LIMIT_PER_MINUTE = 10;

    /**
     * ObjectMapper is thread-safe – using a static instance avoids Spring bean
     * injection timing issues (filter is initialized before Jackson auto-config).
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** Per-IP bucket caches (key = "IP::rule"). Eviction is not needed for typical traffic volumes. */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);

        Integer limitPerMinute = resolveLimit(path);

        // Path is not rate-limited – pass through immediately
        if (limitPerMinute == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(
                clientIp + "::" + limitPerMinute,
                key -> buildBucket(limitPerMinute)
        );

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP {} on path {}", clientIp, path);
            sendTooManyRequestsResponse(response, path);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns the request-per-minute limit for the given path,
     * or {@code null} if the path is not subject to rate limiting.
     */
    private Integer resolveLimit(String path) {
        if (path.equals("/api/auth/login")
                || path.equals("/api/auth/forgot-password")
                || path.equals("/api/auth/reset-password")) {
            return AUTH_LIMIT_PER_MINUTE;
        }
        if (path.startsWith("/api/invitations")) {
            return INVITE_LIMIT_PER_MINUTE;
        }
        return null;
    }

    /**
     * Builds a refilling token bucket with a capacity of {@code requestsPerMinute}
     * tokens that refill fully every 60 seconds.
     */
    private Bucket buildBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the real client IP, accounting for reverse-proxy headers.
     * Falls back to {@link HttpServletRequest#getRemoteAddr()} when no proxy header is present.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; the first entry is the client IP
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a structured JSON 429 response consistent with the project's {@code ErrorResponse} format.
     */
    private void sendTooManyRequestsResponse(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "message", "You have exceeded the request limit. Please wait before trying again.",
                "path", path
        );

        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}
