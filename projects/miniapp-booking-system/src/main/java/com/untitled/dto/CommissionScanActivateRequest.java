package com.untitled.dto;

import javax.validation.constraints.Size;

public class CommissionScanActivateRequest {
    @Size(max = 64)
    private String scene;

    @Size(max = 255)
    private String source;

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
