package com.adwitiya.feedbackportal.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    public enum StorageType {
        LOCAL,

        S3
    }

    private StorageType type = StorageType.LOCAL;

    private String localPath = "./uploads";

    private String bucket = "";

    private String region = "ap-south-1";

    private long maxFileSizeBytes = 5L * 1024 * 1024;

    private List<String> allowedContentTypes = List.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "application/pdf");
}
