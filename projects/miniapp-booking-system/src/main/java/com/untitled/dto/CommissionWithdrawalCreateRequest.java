package com.untitled.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class CommissionWithdrawalCreateRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "提现金额必须大于0")
    private BigDecimal amount;

    @Size(max = 64)
    private String idemKey;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public void setIdemKey(String idemKey) {
        this.idemKey = idemKey;
    }
}
