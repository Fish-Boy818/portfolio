package com.untitled.dto;

public class StorageResult {
    private final String url;
    private final String name;

    public StorageResult(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }
}
