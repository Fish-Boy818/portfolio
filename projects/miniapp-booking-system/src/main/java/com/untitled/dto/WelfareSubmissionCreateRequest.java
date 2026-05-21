package com.untitled.dto;

public class WelfareSubmissionCreateRequest {
    private Long bountyTaskId;
    private Long submissionId;

    private String platformName;

    private String reviewText;

    private String screenshotUrl;

    public Long getBountyTaskId() {
        return bountyTaskId;
    }

    public void setBountyTaskId(Long bountyTaskId) {
        this.bountyTaskId = bountyTaskId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
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
}
