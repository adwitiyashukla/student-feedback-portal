package com.adwitiya.feedbackportal;

import com.adwitiya.feedbackportal.config.properties.AnalyticsProperties;
import com.adwitiya.feedbackportal.config.properties.AppProperties;
import com.adwitiya.feedbackportal.config.properties.JwtProperties;
import com.adwitiya.feedbackportal.config.properties.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        AnalyticsProperties.class,
        StorageProperties.class,
        AppProperties.class
})
public class FeedbackPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeedbackPortalApplication.class, args);
    }
}
