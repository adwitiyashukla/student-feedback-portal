package com.adwitiya.feedbackportal.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.analytics")
public class AnalyticsProperties {
    private boolean enabled = true;

    private String baseUrl = "http://localhost:8000";

    private String apiKey = "";

    private Duration timeout = Duration.ofSeconds(4);
}
