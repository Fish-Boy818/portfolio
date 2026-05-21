package com.untitled.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String type = "tos";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
