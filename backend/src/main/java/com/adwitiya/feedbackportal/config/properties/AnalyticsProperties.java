package com.adwitiya.feedbackportal.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Connection settings for the Python analytics microservice, bound from
 * {@code app.analytics.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.analytics")
public class AnalyticsProperties {

    /** Turn the integration off entirely; feedback is then stored unenriched. */
    private boolean enabled = true;

    /** Base URL of the FastAPI service, e.g. {@code http://analytics:8000}. */
    private String baseUrl = "http://localhost:8000";

    /** Shared secret sent as the {@code X-API-Key} header. */
    private String apiKey = "";

    /** Per-request timeout. The call is best-effort and never blocks a submission. */
    private Duration timeout = Duration.ofSeconds(4);
}
