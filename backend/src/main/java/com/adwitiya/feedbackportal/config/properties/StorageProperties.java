package com.adwitiya.feedbackportal.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Attachment storage configuration, bound from {@code app.storage.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    public enum StorageType {
        /** Write to a directory on the container filesystem. Development only. */
        LOCAL,
        /** Write to an S3 bucket. Used in every deployed environment. */
        S3
    }

    private StorageType type = StorageType.LOCAL;

    /** Directory used when {@link #type} is {@link StorageType#LOCAL}. */
    private String localPath = "./uploads";

    /** Bucket used when {@link #type} is {@link StorageType#S3}. */
    private String bucket = "";

    private String region = "ap-south-1";

    /** Largest accepted upload, in bytes. Defaults to 5 MB. */
    private long maxFileSizeBytes = 5L * 1024 * 1024;

    /** Allow-list of MIME types. Anything else is rejected before it is written. */
    private List<String> allowedContentTypes = List.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "application/pdf");
}
