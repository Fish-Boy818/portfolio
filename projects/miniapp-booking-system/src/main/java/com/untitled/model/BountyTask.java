package com.untitled.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BountyTask {
    private Long id;
    private String location;
    private BigDecimal commissionMin;
    private BigDecimal commissionMax;
    private String coverImageUrl;
    private String detailImageUrl;
    private String stepsJson;
    private String step1Text;
    private String step1ImageUrl;
    private String step2Text;
    private String step2ImageUrl;
    private String step3Text;
    private String step3ImageUrl;
    private String status;
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

    public String getStepsJson() {
        return stepsJson;
    }

    public void setStepsJson(String stepsJson) {
        this.stepsJson = stepsJson;
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
