package com.adwitiya.feedbackportal.security;

import com.adwitiya.feedbackportal.config.properties.AppProperties;
import com.adwitiya.feedbackportal.util.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Fixed-window rate limiter backed by Redis.
 *
 * <p>Authentication endpoints get a tighter budget than the rest of the API,
 * because those are the ones worth brute-forcing. The counter is a Redis
 * {@code INCR} against a key that carries the current minute, so windows
 * expire without a sweeper.</p>
 *
 * <p>If Redis is unreachable the filter <em>fails open</em>: an outage of the
 * cache should degrade protection, not take the whole portal offline.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "ratelimit:";
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        AppProperties.RateLimit config = appProperties.getRateLimit();
        String path = request.getRequestURI();
        boolean authEndpoint = isAuthEndpoint(path);
        int limit = authEndpoint ? config.getLoginAttemptsPerMinute() : config.getApiRequestsPerMinute();

        long used = incrementAndGet(clientKey(request, authEndpoint));
        if (used > limit) {
            log.warn("Rate limit exceeded for {} on {} ({} > {})",
                    LogSanitizer.clean(clientIp(request)), LogSanitizer.clean(path), used, limit);
            rejectWithTooManyRequests(request, response, limit);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - used)));
        filterChain.doFilter(request, response);
    }

    /**
     * @return the new counter value, or {@code 0} when Redis is unavailable so
     *         the caller is never blocked by an infrastructure failure
     */
    private long incrementAndGet(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
            return value == null ? 0L : value;
        } catch (RuntimeException ex) {
            log.debug("Rate limiter unavailable, failing open: {}", ex.getMessage());
            return 0L;
        }
    }

    private void rejectWithTooManyRequests(HttpServletRequest request, HttpServletResponse response, int limit)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "type", URI.create("https://feedback-portal/errors/rate-limited").toString(),
                "title", "Too Many Requests",
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "detail", "Rate limit of %d requests per minute exceeded.".formatted(limit),
                "instance", request.getRequestURI(),
                "timestamp", Instant.now().toString()));
    }

    private String clientKey(HttpServletRequest request, boolean authEndpoint) {
        long minute = Instant.now().getEpochSecond() / WINDOW.toSeconds();
        return KEY_PREFIX + (authEndpoint ? "auth:" : "api:") + clientIp(request) + ":" + minute;
    }

    /** Honours {@code X-Forwarded-For} so the limiter works behind a load balancer. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/v1/auth/");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!appProperties.getRateLimit().isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/v1/health");
    }
}
