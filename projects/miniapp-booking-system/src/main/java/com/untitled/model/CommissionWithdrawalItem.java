package com.untitled.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommissionWithdrawalItem {
    private Long id;
    private Long withdrawalId;
    private Long commissionRecordId;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWithdrawalId() {
        return withdrawalId;
    }

    public void setWithdrawalId(Long withdrawalId) {
        this.withdrawalId = withdrawalId;
    }

    public Long getCommissionRecordId() {
        return commissionRecordId;
    }

    public void setCommissionRecordId(Long commissionRecordId) {
        this.commissionRecordId = commissionRecordId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
