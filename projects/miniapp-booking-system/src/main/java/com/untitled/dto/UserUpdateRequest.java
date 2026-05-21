package com.untitled.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class UserUpdateRequest {
    @NotBlank
    private String nickname;

    @Size(max = 255)
    private String avatarUrl;

    @Size(max = 32)
    private String phone;

    @Min(1)
    @Max(3)
    private Integer depth;

    private Integer scanUser;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getScanUser() {
        return scanUser;
    }

    public void setScanUser(Integer scanUser) {
        this.scanUser = scanUser;
    }

}
