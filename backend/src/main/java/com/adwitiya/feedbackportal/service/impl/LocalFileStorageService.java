package com.adwitiya.feedbackportal.service.impl;

import com.adwitiya.feedbackportal.config.properties.StorageProperties;
import com.adwitiya.feedbackportal.exception.BusinessRuleException;
import com.adwitiya.feedbackportal.exception.StorageException;
import com.adwitiya.feedbackportal.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores attachments on the container filesystem. Development default.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties properties;
    private Path rootDirectory;

    @PostConstruct
    void createRootDirectory() {
        try {
            this.rootDirectory = Paths.get(properties.getLocalPath()).toAbsolutePath().normalize();
            Files.createDirectories(rootDirectory);
            log.info("Local attachment storage rooted at {}", rootDirectory);
        } catch (IOException ex) {
            throw new StorageException("Could not create the attachment directory", ex);
        }
    }

    @Override
    public String store(MultipartFile file, String keyPrefix) {
        validate(file);
        String storageKey = "%s/%s%s".formatted(keyPrefix, UUID.randomUUID(), extensionOf(file));
        Path target = resolveWithinRoot(storageKey);

        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return storageKey;
        } catch (IOException ex) {
            throw new StorageException("Could not store " + file.getOriginalFilename(), ex);
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return Files.readAllBytes(resolveWithinRoot(storageKey));
        } catch (IOException ex) {
            throw new StorageException("Could not read attachment " + storageKey, ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveWithinRoot(storageKey));
        } catch (IOException ex) {
            log.warn("Could not delete attachment {}: {}", storageKey, ex.getMessage());
        }
    }

    /**
     * Resolves a key against the storage root and refuses to escape it.
     *
     * <p>Without this check a key of {@code ../../etc/passwd} would be read
     * straight off the host filesystem.</p>
     */
    private Path resolveWithinRoot(String storageKey) {
        Path resolved = rootDirectory.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new StorageException("Rejected path traversal attempt in storage key: " + storageKey);
        }
        return resolved;
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
