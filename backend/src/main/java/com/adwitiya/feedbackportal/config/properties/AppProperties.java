package com.adwitiya.feedbackportal.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * General application settings, bound from {@code app.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Public base URL, used to build links inside notification emails. */
    private String baseUrl = "http://localhost:8080";

    /** Origins permitted to call the REST API from a browser. */
    private List<String> allowedOrigins = List.of("http://localhost:8080", "http://localhost:5173");

    /** From-address for outbound mail. */
    private String mailFrom = "no-reply@university.edu";

    /** Automatically close RESOLVED feedback the student has not responded to. */
    private Duration autoCloseAfter = Duration.ofDays(7);

    private final Bootstrap bootstrap = new Bootstrap();
    private final RateLimit rateLimit = new RateLimit();

    /**
     * Credentials for the super-administrator created on an empty database, so
     * a fresh deployment has exactly one way in and no anonymous registration
     * endpoint.
     */
    @Getter
    @Setter
    public static class Bootstrap {
        private boolean enabled = true;
        private String email = "admin@university.edu";
        private String password = "";
    }

    /** Fixed-window rate limiting applied to authentication endpoints. */
    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private int loginAttemptsPerMinute = 10;
        private int apiRequestsPerMinute = 120;
    }
}
