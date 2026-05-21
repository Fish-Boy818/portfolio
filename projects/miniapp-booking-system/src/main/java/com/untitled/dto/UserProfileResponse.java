package com.untitled.dto;

import java.math.BigDecimal;

public class UserProfileResponse {
    private Long id;
    private String openId;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private BigDecimal totalSpend;
    private Integer priceLevel;
    private BigDecimal remainToUnlock;
    private Boolean scanUser;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(BigDecimal totalSpend) {
        this.totalSpend = totalSpend;
    }

    public Integer getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }

    public BigDecimal getRemainToUnlock() {
        return remainToUnlock;
    }

    public void setRemainToUnlock(BigDecimal remainToUnlock) {
        this.remainToUnlock = remainToUnlock;
    }

    public Boolean getScanUser() {
        return scanUser;
    }

    public void setScanUser(Boolean scanUser) {
        this.scanUser = scanUser;
    }
}
