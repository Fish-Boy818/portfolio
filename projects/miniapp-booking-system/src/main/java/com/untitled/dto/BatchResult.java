package com.untitled.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchResult {
    private int total;
    private int success;
    private int failed;
    private List<String> errors = new ArrayList<>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String message) {
        if (message == null) {
            return;
        }
        errors.add(message);
    }
}
