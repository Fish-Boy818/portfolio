package com.untitled.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommissionWithdrawalResponse {
    private Long id;
    private String withdrawNo;
    private Long userId;
    private String userPhone;
    private BigDecimal amount;
    private String status;
    private String statusText;
    private String failReason;
    private String transferBillNo;
    private String transferState;
    private String transferPackageInfo;
    private String transferMchId;
    private String transferAppId;
    private String operatorName;
    private String operatorNote;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWithdrawNo() {
        return withdrawNo;
    }

    public void setWithdrawNo(String withdrawNo) {
        this.withdrawNo = withdrawNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getTransferBillNo() {
        return transferBillNo;
    }

    public void setTransferBillNo(String transferBillNo) {
        this.transferBillNo = transferBillNo;
    }

    public String getTransferState() {
        return transferState;
    }

    public void setTransferState(String transferState) {
        this.transferState = transferState;
    }

    public String getTransferPackageInfo() {
        return transferPackageInfo;
    }

    public void setTransferPackageInfo(String transferPackageInfo) {
        this.transferPackageInfo = transferPackageInfo;
    }

    public String getTransferMchId() {
        return transferMchId;
    }

    public void setTransferMchId(String transferMchId) {
        this.transferMchId = transferMchId;
    }

    public String getTransferAppId() {
        return transferAppId;
    }

    public void setTransferAppId(String transferAppId) {
        this.transferAppId = transferAppId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorNote() {
        return operatorNote;
    }

    public void setOperatorNote(String operatorNote) {
        this.operatorNote = operatorNote;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
