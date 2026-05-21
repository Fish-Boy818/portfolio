package com.untitled.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long clubId;
    private Long slotId;
    private String phone;
    private String title;
    private String clubName;
    private String clubLocation;
    private LocalDate slotDate;
    private String slotTime;
    private String status;
    private Integer priceLevel;
    private BigDecimal unitPrice;
    private BigDecimal originalPrice;
    private Integer quantity;
    private BigDecimal payAmount;
    private Integer scanUserAtOrderTime;
    private BigDecimal merchantSettlementAmount;
    private BigDecimal platformOperationFee;
    private BigDecimal userCommissionAmount;
    private String verifyCode;
    private LocalDateTime paidAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime refundAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public String getSlotTime() {
        return slotTime;
    }

    public void setSlotTime(String slotTime) {
        this.slotTime = slotTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public Integer getScanUserAtOrderTime() {
        return scanUserAtOrderTime;
    }

    public void setScanUserAtOrderTime(Integer scanUserAtOrderTime) {
        this.scanUserAtOrderTime = scanUserAtOrderTime;
    }

    public BigDecimal getMerchantSettlementAmount() {
        return merchantSettlementAmount;
    }

    public void setMerchantSettlementAmount(BigDecimal merchantSettlementAmount) {
        this.merchantSettlementAmount = merchantSettlementAmount;
    }

    public BigDecimal getPlatformOperationFee() {
        return platformOperationFee;
    }

    public void setPlatformOperationFee(BigDecimal platformOperationFee) {
        this.platformOperationFee = platformOperationFee;
    }

    public BigDecimal getUserCommissionAmount() {
        return userCommissionAmount;
    }

    public void setUserCommissionAmount(BigDecimal userCommissionAmount) {
        this.userCommissionAmount = userCommissionAmount;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getRefundAt() {
        return refundAt;
    }

    public void setRefundAt(LocalDateTime refundAt) {
        this.refundAt = refundAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
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
