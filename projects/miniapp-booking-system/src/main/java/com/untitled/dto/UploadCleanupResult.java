package com.untitled.dto;

import java.util.ArrayList;
import java.util.List;

public class UploadCleanupResult {
    private boolean dryRun;
    private boolean skipped;
    private String message;
    private int totalFiles;
    private int usedFiles;
    private int unusedFiles;
    private List<String> deletedFiles = new ArrayList<>();
    private List<String> keptFiles = new ArrayList<>();

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

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

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getUsedFiles() {
        return usedFiles;
    }

    public void setUsedFiles(int usedFiles) {
        this.usedFiles = usedFiles;
    }

    public int getUnusedFiles() {
        return unusedFiles;
    }

    public void setUnusedFiles(int unusedFiles) {
        this.unusedFiles = unusedFiles;
    }

    public List<String> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<String> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public List<String> getKeptFiles() {
        return keptFiles;
    }

    public void setKeptFiles(List<String> keptFiles) {
        this.keptFiles = keptFiles;
    }
}
