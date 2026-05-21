package com.untitled.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "commission")
public class CommissionProperties {
    private Withdraw withdraw = new Withdraw();

    public Withdraw getWithdraw() {
        return withdraw;
    }

    public void setWithdraw(Withdraw withdraw) {
        this.withdraw = withdraw;
    }

    public static class Withdraw {
        private BigDecimal minAmount = new BigDecimal("50");
        private BigDecimal singleMaxAmount = new BigDecimal("200");
        private Integer dailyMaxCount = 1;
        private BigDecimal dailyMaxAmount = new BigDecimal("1000");

        public BigDecimal getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(BigDecimal minAmount) {
            this.minAmount = minAmount;
        }

        public Integer getDailyMaxCount() {
            return dailyMaxCount;
        }

        public void setDailyMaxCount(Integer dailyMaxCount) {
            this.dailyMaxCount = dailyMaxCount;
        }

        public BigDecimal getSingleMaxAmount() {
            return singleMaxAmount;
        }

        public void setSingleMaxAmount(BigDecimal singleMaxAmount) {
            this.singleMaxAmount = singleMaxAmount;
        }

        public BigDecimal getDailyMaxAmount() {
            return dailyMaxAmount;
        }

        public void setDailyMaxAmount(BigDecimal dailyMaxAmount) {
            this.dailyMaxAmount = dailyMaxAmount;
        }
    }
}
