package com.untitled.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommissionAccount {
    private Long userId;
    private BigDecimal withdrawableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal withdrawingBalance;
    private BigDecimal withdrawnTotal;
    private BigDecimal reversedTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getWithdrawableBalance() {
        return withdrawableBalance;
    }

    public void setWithdrawableBalance(BigDecimal withdrawableBalance) {
        this.withdrawableBalance = withdrawableBalance;
    }

    public BigDecimal getPendingBalance() {
        return pendingBalance;
    }

    public void setPendingBalance(BigDecimal pendingBalance) {
        this.pendingBalance = pendingBalance;
    }

    public BigDecimal getWithdrawingBalance() {
        return withdrawingBalance;
    }

    public void setWithdrawingBalance(BigDecimal withdrawingBalance) {
        this.withdrawingBalance = withdrawingBalance;
    }

    public BigDecimal getWithdrawnTotal() {
        return withdrawnTotal;
    }

    public void setWithdrawnTotal(BigDecimal withdrawnTotal) {
        this.withdrawnTotal = withdrawnTotal;
    }

    public BigDecimal getReversedTotal() {
        return reversedTotal;
    }

    public void setReversedTotal(BigDecimal reversedTotal) {
        this.reversedTotal = reversedTotal;
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
