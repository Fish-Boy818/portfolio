package com.untitled.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class BountyTaskUpdateRequest {
    @NotBlank(message = "地点不能为空")
    private String location;

    @NotNull(message = "最低佣金不能为空")
    @DecimalMin(value = "0.00", message = "最低佣金不能小于0")
    private BigDecimal commissionMin;

    @NotNull(message = "最高佣金不能为空")
    @DecimalMin(value = "0.00", message = "最高佣金不能小于0")
    private BigDecimal commissionMax;

    @NotBlank(message = "封面图不能为空")
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
    private Integer sort;

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

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
