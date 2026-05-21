package com.untitled.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WelfareSubmissionResponse {
    private Long id;
    private String submissionNo;
    private Long userId;
    private String userPhone;
    private String userNickname;
    private Long bountyTaskId;
    private String bountyLocation;
    private String bountyLocationAttemptText;
    private Integer submissionAttempt;
    private String platformName;
    private String reviewText;
    private String screenshotUrl;
    private String status;
    private String statusText;
    private BigDecimal rewardAmount;
    private String reviewNote;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private Long rewardRecordId;
    private LocalDateTime rewardedAt;
    private LocalDateTime deleteImageAt;
    private Boolean imageDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubmissionNo() {
        return submissionNo;
    }

    public void setSubmissionNo(String submissionNo) {
        this.submissionNo = submissionNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public Long getBountyTaskId() {
        return bountyTaskId;
    }

    public void setBountyTaskId(Long bountyTaskId) {
        this.bountyTaskId = bountyTaskId;
    }

    public String getBountyLocation() {
        return bountyLocation;
    }

    public void setBountyLocation(String bountyLocation) {
        this.bountyLocation = bountyLocation;
    }

    public String getBountyLocationAttemptText() {
        return bountyLocationAttemptText;
    }

    public void setBountyLocationAttemptText(String bountyLocationAttemptText) {
        this.bountyLocationAttemptText = bountyLocationAttemptText;
    }

    public Integer getSubmissionAttempt() {
        return submissionAttempt;
    }

    public void setSubmissionAttempt(Integer submissionAttempt) {
        this.submissionAttempt = submissionAttempt;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getScreenshotUrl() {
        return screenshotUrl;
    }

    public void setScreenshotUrl(String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(BigDecimal rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getRewardRecordId() {
        return rewardRecordId;
    }

    public void setRewardRecordId(Long rewardRecordId) {
        this.rewardRecordId = rewardRecordId;
    }

    public LocalDateTime getRewardedAt() {
        return rewardedAt;
    }

    public void setRewardedAt(LocalDateTime rewardedAt) {
        this.rewardedAt = rewardedAt;
    }

    public LocalDateTime getDeleteImageAt() {
        return deleteImageAt;
    }

    public void setDeleteImageAt(LocalDateTime deleteImageAt) {
        this.deleteImageAt = deleteImageAt;
    }

    public Boolean getImageDeleted() {
        return imageDeleted;
    }

    public void setImageDeleted(Boolean imageDeleted) {
        this.imageDeleted = imageDeleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
