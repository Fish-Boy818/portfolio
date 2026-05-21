package com.untitled.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BountyTaskResponse {
    private Long id;
    private String location;
    private BigDecimal commissionMin;
    private BigDecimal commissionMax;
    private String commissionRangeText;
    private String coverImageUrl;
    private String detailImageUrl;
    private List<BountyTaskStepItem> steps;
    private String step1Text;
    private String step1ImageUrl;
    private String step2Text;
    private String step2ImageUrl;
    private String step3Text;
    private String step3ImageUrl;
    private String status;
    private String statusText;
    private String claimStatus;
    private String claimStatusText;
    private Integer submitCount;
    private String actionText;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long latestSubmissionId;
    private String latestSubmissionStatus;
    private String latestSubmissionStatusText;
    private BigDecimal latestSubmissionRewardAmount;
    private String latestSubmissionReviewNote;
    private String latestSubmissionScreenshotUrl;
    private LocalDateTime latestSubmissionCreatedAt;
    private Integer totalClaims;
    private Integer pendingReviewClaims;
    private Integer completedClaims;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getCommissionMin() {
        return commissionMin;
    }

    public void setCommissionMin(BigDecimal commissionMin) {
        this.commissionMin = commissionMin;
    }

    public BigDecimal getCommissionMax() {
        return commissionMax;
    }

    public void setCommissionMax(BigDecimal commissionMax) {
        this.commissionMax = commissionMax;
    }

    public String getCommissionRangeText() {
        return commissionRangeText;
    }

    public void setCommissionRangeText(String commissionRangeText) {
        this.commissionRangeText = commissionRangeText;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getDetailImageUrl() {
        return detailImageUrl;
    }

    public void setDetailImageUrl(String detailImageUrl) {
        this.detailImageUrl = detailImageUrl;
    }

    public List<BountyTaskStepItem> getSteps() {
        return steps;
    }

    public void setSteps(List<BountyTaskStepItem> steps) {
        this.steps = steps;
    }

    public String getStep1Text() {
        return step1Text;
    }

    public void setStep1Text(String step1Text) {
        this.step1Text = step1Text;
    }

    public String getStep1ImageUrl() {
        return step1ImageUrl;
    }

    public void setStep1ImageUrl(String step1ImageUrl) {
        this.step1ImageUrl = step1ImageUrl;
    }

    public String getStep2Text() {
        return step2Text;
    }

    public void setStep2Text(String step2Text) {
        this.step2Text = step2Text;
    }

    public String getStep2ImageUrl() {
        return step2ImageUrl;
    }

    public void setStep2ImageUrl(String step2ImageUrl) {
        this.step2ImageUrl = step2ImageUrl;
    }

    public String getStep3Text() {
        return step3Text;
    }

    public void setStep3Text(String step3Text) {
        this.step3Text = step3Text;
    }

    public String getStep3ImageUrl() {
        return step3ImageUrl;
    }

    public void setStep3ImageUrl(String step3ImageUrl) {
        this.step3ImageUrl = step3ImageUrl;
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

    public String getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(String claimStatus) {
        this.claimStatus = claimStatus;
    }

    public String getClaimStatusText() {
        return claimStatusText;
    }

    public void setClaimStatusText(String claimStatusText) {
        this.claimStatusText = claimStatusText;
    }

    public Integer getSubmitCount() {
        return submitCount;
    }

    public void setSubmitCount(Integer submitCount) {
        this.submitCount = submitCount;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getLatestSubmissionId() {
        return latestSubmissionId;
    }

    public void setLatestSubmissionId(Long latestSubmissionId) {
        this.latestSubmissionId = latestSubmissionId;
    }

    public String getLatestSubmissionStatus() {
        return latestSubmissionStatus;
    }

    public void setLatestSubmissionStatus(String latestSubmissionStatus) {
        this.latestSubmissionStatus = latestSubmissionStatus;
    }

    public String getLatestSubmissionStatusText() {
        return latestSubmissionStatusText;
    }

    public void setLatestSubmissionStatusText(String latestSubmissionStatusText) {
        this.latestSubmissionStatusText = latestSubmissionStatusText;
    }

    public BigDecimal getLatestSubmissionRewardAmount() {
        return latestSubmissionRewardAmount;
    }

    public void setLatestSubmissionRewardAmount(BigDecimal latestSubmissionRewardAmount) {
        this.latestSubmissionRewardAmount = latestSubmissionRewardAmount;
    }

    public String getLatestSubmissionReviewNote() {
        return latestSubmissionReviewNote;
    }

    public void setLatestSubmissionReviewNote(String latestSubmissionReviewNote) {
        this.latestSubmissionReviewNote = latestSubmissionReviewNote;
    }

    public String getLatestSubmissionScreenshotUrl() {
        return latestSubmissionScreenshotUrl;
    }

    public void setLatestSubmissionScreenshotUrl(String latestSubmissionScreenshotUrl) {
        this.latestSubmissionScreenshotUrl = latestSubmissionScreenshotUrl;
    }

    public LocalDateTime getLatestSubmissionCreatedAt() {
        return latestSubmissionCreatedAt;
    }

    public void setLatestSubmissionCreatedAt(LocalDateTime latestSubmissionCreatedAt) {
        this.latestSubmissionCreatedAt = latestSubmissionCreatedAt;
    }

    public Integer getTotalClaims() {
        return totalClaims;
    }

    public void setTotalClaims(Integer totalClaims) {
        this.totalClaims = totalClaims;
    }

    public Integer getPendingReviewClaims() {
        return pendingReviewClaims;
    }

    public void setPendingReviewClaims(Integer pendingReviewClaims) {
        this.pendingReviewClaims = pendingReviewClaims;
    }

    public Integer getCompletedClaims() {
        return completedClaims;
    }

    public void setCompletedClaims(Integer completedClaims) {
        this.completedClaims = completedClaims;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
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
