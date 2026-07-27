package com.adwitiya.feedbackportal.service.impl;

import com.adwitiya.feedbackportal.config.properties.StorageProperties;
import com.adwitiya.feedbackportal.exception.BusinessRuleException;
import com.adwitiya.feedbackportal.exception.StorageException;
import com.adwitiya.feedbackportal.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores attachments in S3. Used in every deployed environment.
 *
 * <p>Credentials come from the default provider chain, which on ECS resolves
 * to the task role - no access keys are configured anywhere in this
 * repository. Objects are written with SSE-S3 encryption at rest.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "S3")
public class S3FileStorageService implements FileStorageService {

    private final StorageProperties properties;
    private final S3Client s3Client;

    public S3FileStorageService(StorageProperties properties) {
        this.properties = properties;
        this.s3Client = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
        log.info("S3 attachment storage using bucket '{}' in {}", properties.getBucket(), properties.getRegion());
    }

    @Override
    public String store(MultipartFile file, String keyPrefix) {
        validate(file);
        String storageKey = "%s/%s%s".formatted(keyPrefix, UUID.randomUUID(), extensionOf(file));

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return storageKey;
        } catch (IOException | S3Exception ex) {
            throw new StorageException("Could not upload " + file.getOriginalFilename() + " to S3", ex);
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build()).asByteArray();
        } catch (S3Exception ex) {
            throw new StorageException("Could not download attachment " + storageKey, ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
        } catch (S3Exception ex) {
            log.warn("Could not delete S3 object {}: {}", storageKey, ex.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("The uploaded file is empty");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BusinessRuleException("File exceeds the %d MB limit"
                    .formatted(properties.getMaxFileSizeBytes() / (1024 * 1024)));
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("Unsupported file type: " + contentType);
        }
    }

    private String extensionOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        String extension = name.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,8}") ? extension : "";
    }
}
