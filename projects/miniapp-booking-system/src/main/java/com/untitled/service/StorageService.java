package com.untitled.service;

import com.untitled.config.StorageProperties;
import com.untitled.dto.StorageResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class StorageService {
    private final StorageProperties storageProperties;
    private final LocalStorageService localStorageService;
    private final TosStorageService tosStorageService;

    public StorageService(StorageProperties storageProperties,
                          LocalStorageService localStorageService,
                          TosStorageService tosStorageService) {
        this.storageProperties = storageProperties;
        this.localStorageService = localStorageService;
        this.tosStorageService = tosStorageService;
    }

    public StorageResult upload(MultipartFile file) throws IOException {
        return upload(file, null);
    }

    public StorageResult upload(MultipartFile file, Integer maxVideoSeconds) throws IOException {
        if ("local".equalsIgnoreCase(storageProperties.getType())) {
            return localStorageService.upload(file, maxVideoSeconds);
        }
        return tosStorageService.upload(file);
    }

    public boolean delete(String urlOrName) throws IOException {
        if ("local".equalsIgnoreCase(storageProperties.getType())) {
            return localStorageService.delete(urlOrName);
        }
        return false;
    }
}
