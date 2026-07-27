package com.adwitiya.feedbackportal.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over attachment storage.
 *
 * <p>Two implementations exist — local disk for development and S3 for
 * deployed environments — selected by {@code app.storage.type}. Callers only
 * ever see an opaque storage key.</p>
 */
public interface FileStorageService {

    /**
     * Validates and stores an uploaded file.
     *
     * @param file      the upload
     * @param keyPrefix logical folder, e.g. {@code feedback/42}
     * @return the storage key needed to read the file back
     */
    String store(MultipartFile file, String keyPrefix);

    /**
     * @param storageKey key returned by {@link #store}
     * @return the file contents
     */
    byte[] retrieve(String storageKey);

    void delete(String storageKey);
}
