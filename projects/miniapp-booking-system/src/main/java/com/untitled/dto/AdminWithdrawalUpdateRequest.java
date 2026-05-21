package com.untitled.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AdminWithdrawalUpdateRequest {
    @NotBlank
    @Size(max = 20)
    private String status;

    @Size(max = 255)
    private String failReason;

    @Size(max = 64)
    private String operatorName;

    @Size(max = 255)
    private String operatorNote;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
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
}
