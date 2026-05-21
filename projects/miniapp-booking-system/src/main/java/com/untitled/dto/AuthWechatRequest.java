package com.untitled.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AuthWechatRequest {
    @NotBlank
    private String code;

    @Size(max = 64)
    private String nickname;

    @Size(max = 255)
    private String avatarUrl;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
