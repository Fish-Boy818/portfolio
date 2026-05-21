package com.untitled.dto;

public class InviteOverviewResponse {
    private Long userId;
    private Long inviterId;
    private Boolean scanUser;
    private Integer inviteCount;
    private String inviteCode;
    private String qrCodePath;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getInviterId() {
        return inviterId;
    }

    public void setInviterId(Long inviterId) {
        this.inviterId = inviterId;
    }

    public Boolean getScanUser() {
        return scanUser;
    }

    public void setScanUser(Boolean scanUser) {
        this.scanUser = scanUser;
    }

    public Integer getInviteCount() {
        return inviteCount;
    }

    public void setInviteCount(Integer inviteCount) {
        this.inviteCount = inviteCount;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getQrCodePath() {
        return qrCodePath;
    }

    public void setQrCodePath(String qrCodePath) {
        this.qrCodePath = qrCodePath;
    }
}
