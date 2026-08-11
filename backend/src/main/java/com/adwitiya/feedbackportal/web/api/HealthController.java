package com.adwitiya.feedbackportal.web.api;

import com.adwitiya.feedbackportal.integration.AnalyticsClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Health", description = "Service health")
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {
    private final AnalyticsClient analyticsClient;

    @Operation(summary = "Liveness probe", security = @SecurityRequirement(name = ""))
    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "student-feedback-portal",
                "version", "2.0.0",
                "timestamp", Instant.now().toString());
    }

    @Operation(summary = "Downstream dependency status")
    @GetMapping("/dependencies")
    public Map<String, Object> dependencies() {
        return Map.of("analyticsService", analyticsClient.isHealthy() ? "UP" : "DOWN");
    }
}
