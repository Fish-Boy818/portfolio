package com.untitled.dto;

import java.math.BigDecimal;

public class DashboardStats {
    private BigDecimal totalSales;
    private BigDecimal totalVerified;
    private BigDecimal totalUnverified;
    private BigDecimal totalWithdrawnCommission;
    private BigDecimal totalPendingCommission;
    private int totalUsers;

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getTotalVerified() {
        return totalVerified;
    }

    public void setTotalVerified(BigDecimal totalVerified) {
        this.totalVerified = totalVerified;
    }

    public BigDecimal getTotalUnverified() {
        return totalUnverified;
    }

    public void setTotalUnverified(BigDecimal totalUnverified) {
        this.totalUnverified = totalUnverified;
    }

    public BigDecimal getTotalWithdrawnCommission() {
        return totalWithdrawnCommission;
    }

    public void setTotalWithdrawnCommission(BigDecimal totalWithdrawnCommission) {
        this.totalWithdrawnCommission = totalWithdrawnCommission;
    }

    public BigDecimal getTotalPendingCommission() {
        return totalPendingCommission;
    }

    public void setTotalPendingCommission(BigDecimal totalPendingCommission) {
        this.totalPendingCommission = totalPendingCommission;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }
}
