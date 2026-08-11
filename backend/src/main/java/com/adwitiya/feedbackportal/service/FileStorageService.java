package com.adwitiya.feedbackportal.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String keyPrefix);

    byte[] retrieve(String storageKey);

    void delete(String storageKey);
}
