package com.untitled.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AdminCredentialUpdateRequest {
    @NotBlank(message = "username 不能为空")
    @Size(max = 64, message = "username 长度不能超过64")
    private String username;

    @NotBlank(message = "currentPassword 不能为空")
    @Size(max = 128, message = "currentPassword 长度不能超过128")
    private String currentPassword;

    @NotBlank(message = "password 不能为空")
    @Size(max = 128, message = "password 长度不能超过128")
    private String password;

    @NotBlank(message = "confirmPassword 不能为空")
    @Size(max = 128, message = "confirmPassword 长度不能超过128")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
