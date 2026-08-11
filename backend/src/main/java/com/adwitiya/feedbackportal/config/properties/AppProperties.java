package com.adwitiya.feedbackportal.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String baseUrl = "http://localhost:8080";

    private List<String> allowedOrigins = List.of("http://localhost:8080", "http://localhost:5173");

    private String mailFrom = "no-reply@university.edu";

    private Duration autoCloseAfter = Duration.ofDays(7);

    private final Bootstrap bootstrap = new Bootstrap();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Bootstrap {
        private boolean enabled = true;
        private String email = "admin@university.edu";
        private String password = "";
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private int loginAttemptsPerMinute = 10;
        private int apiRequestsPerMinute = 120;
    }
}
