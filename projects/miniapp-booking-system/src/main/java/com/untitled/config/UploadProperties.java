package com.untitled.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app")
public class UploadProperties {
    private String uploadDir = "uploads";

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public Path resolvePath() {
        Path dir = Paths.get(uploadDir);
        if (dir.isAbsolute()) {
            return dir;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(uploadDir);
    }
}
