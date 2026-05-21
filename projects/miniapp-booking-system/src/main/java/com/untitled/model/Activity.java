package com.untitled.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Activity {
    private Long id;
    private Long clubId;
    private String title;
    private String subtitle;
    private String category;
    private BigDecimal basePrice;
    private BigDecimal originalPrice;
    private BigDecimal merchantSettlementPrice;
    private BigDecimal platformOperationFee;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal buyerCommissionAmount;
    private BigDecimal inviterCommissionAmount;
    private String audience;
    private String description;
    private String bundle;
    private LocalDate expireDate;
    private String status;
    private String cover;
    private String gallery;
    private String detailImages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getMerchantSettlementPrice() {
        return merchantSettlementPrice;
    }

    public void setMerchantSettlementPrice(BigDecimal merchantSettlementPrice) {
        this.merchantSettlementPrice = merchantSettlementPrice;
    }

    public BigDecimal getPlatformOperationFee() {
        return platformOperationFee;
    }

    public void setPlatformOperationFee(BigDecimal platformOperationFee) {
        this.platformOperationFee = platformOperationFee;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getBuyerCommissionAmount() {
        return buyerCommissionAmount;
    }

    public void setBuyerCommissionAmount(BigDecimal buyerCommissionAmount) {
        this.buyerCommissionAmount = buyerCommissionAmount;
    }

    public BigDecimal getInviterCommissionAmount() {
        return inviterCommissionAmount;
    }

    public void setInviterCommissionAmount(BigDecimal inviterCommissionAmount) {
        this.inviterCommissionAmount = inviterCommissionAmount;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getGallery() {
        return gallery;
    }

    public void setGallery(String gallery) {
        this.gallery = gallery;
    }

    public String getDetailImages() {
        return detailImages;
    }

    public void setDetailImages(String detailImages) {
        this.detailImages = detailImages;
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
