package com.adwitiya.feedbackportal.integration;

import com.adwitiya.feedbackportal.config.properties.AnalyticsProperties;
import com.adwitiya.feedbackportal.web.dto.response.AnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/**
 * Client for the Python analytics microservice.
 *
 * <p>The integration is <em>advisory</em>. Sentiment and category suggestions
 * improve triage, but feedback must be accepted whether or not the model is
 * reachable, so every failure path returns {@link Optional#empty()} and the
 * caller carries on.</p>
 */
@Slf4j
@Component
public class AnalyticsClient {

    private final RestClient restClient;
    private final AnalyticsProperties properties;

    public AnalyticsClient(@Qualifier("analyticsRestClient") RestClient restClient,
                           AnalyticsProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Classifies a piece of feedback.
     *
     * @param title       the feedback title
     * @param description the feedback body
     * @return the model's assessment, or empty if analytics is disabled or unreachable
     */
    public Optional<AnalysisResult> analyse(String title, String description) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            AnalysisResult result = restClient.post()
                    .uri("/api/v1/analyze")
                    .body(Map.of("title", title, "text", description))
                    .retrieve()
                    .body(AnalysisResult.class);

            if (result == null) {
                log.warn("Analytics service returned an empty body");
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (RestClientException ex) {
            log.warn("Analytics service unavailable, storing feedback unenriched: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** @return whether the analytics service answers its health probe */
    public boolean isHealthy() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            restClient.get().uri("/api/v1/health").retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            return false;
        }
    }
}
