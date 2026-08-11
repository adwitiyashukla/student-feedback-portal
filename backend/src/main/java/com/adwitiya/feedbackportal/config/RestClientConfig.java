package com.adwitiya.feedbackportal.config;

import com.adwitiya.feedbackportal.config.properties.AnalyticsProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final AnalyticsProperties analyticsProperties;

    @Bean("analyticsRestClient")
    public RestClient analyticsRestClient(RestClient.Builder builder) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(analyticsProperties.getTimeout())
                .withReadTimeout(analyticsProperties.getTimeout());
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return builder
                .baseUrl(analyticsProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-API-Key", analyticsProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public RestClientCustomizer defaultRestClientCustomizer() {
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(java.time.Duration.ofSeconds(5))
                        .withReadTimeout(java.time.Duration.ofSeconds(10))));
    }
}
