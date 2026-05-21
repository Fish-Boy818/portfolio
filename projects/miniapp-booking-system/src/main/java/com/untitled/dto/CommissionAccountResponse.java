package com.untitled.dto;

import java.math.BigDecimal;

public class CommissionAccountResponse {
    private Boolean scanUser;
    private BigDecimal withdrawableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal withdrawingBalance;
    private BigDecimal withdrawnTotal;
    private BigDecimal reversedTotal;

    public Boolean getScanUser() {
        return scanUser;
    }

    public void setScanUser(Boolean scanUser) {
        this.scanUser = scanUser;
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
}
