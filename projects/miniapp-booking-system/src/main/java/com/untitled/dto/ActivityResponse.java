package com.untitled.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ActivityResponse {
    private Long id;
    private Long clubId;
    private String clubName;
    private String clubLocation;
    private String title;
    private String subtitle;
    private String category;
    private BigDecimal price;
    private BigDecimal original;
    private BigDecimal basePrice;
    private BigDecimal originalPrice;
    private BigDecimal merchantSettlementPrice;
    private BigDecimal platformOperationFee;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal buyerCommissionAmount;
    private BigDecimal inviterCommissionAmount;
    private BigDecimal expectedCommission;
    private String commissionTipText;
    private Boolean showCommission;
    private Integer priceLevel;
    private String audience;
    private String description;
    private String bundle;
    private LocalDate expireDate;
    private String status;
    private String cover;
    private List<String> gallery;
    private List<String> detailImages;

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

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getClubLocation() {
        return clubLocation;
    }

    public void setClubLocation(String clubLocation) {
        this.clubLocation = clubLocation;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginal() {
        return original;
    }

    public void setOriginal(BigDecimal original) {
        this.original = original;
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

    public BigDecimal getExpectedCommission() {
        return expectedCommission;
    }

    public void setExpectedCommission(BigDecimal expectedCommission) {
        this.expectedCommission = expectedCommission;
    }

    public String getCommissionTipText() {
        return commissionTipText;
    }

    public void setCommissionTipText(String commissionTipText) {
        this.commissionTipText = commissionTipText;
    }

    public Boolean getShowCommission() {
        return showCommission;
    }

    public void setShowCommission(Boolean showCommission) {
        this.showCommission = showCommission;
    }

    public Integer getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
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

    public List<String> getGallery() {
        return gallery;
    }

    public void setGallery(List<String> gallery) {
        this.gallery = gallery;
    }

    public List<String> getDetailImages() {
        return detailImages;
    }

    public void setDetailImages(List<String> detailImages) {
        this.detailImages = detailImages;
    }
}
