package com.untitled.dto;

import java.util.ArrayList;
import java.util.List;

public class UploadMissingResult {
    private boolean skipped;
    private String message;
    private int totalReferences;
    private int missingCount;
    private List<UploadMissingItem> items = new ArrayList<>();

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalReferences() {
        return totalReferences;
    }

    public void setTotalReferences(int totalReferences) {
        this.totalReferences = totalReferences;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public List<UploadMissingItem> getItems() {
        return items;
    }

    public void setItems(List<UploadMissingItem> items) {
        this.items = items;
    }
}
