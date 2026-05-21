package com.untitled.dto;

import javax.validation.constraints.NotBlank;

public class OrderUpdateRequest {
    @NotBlank
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
