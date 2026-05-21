package com.untitled.model;

public class BountyClaimStat {
    private Long bountyTaskId;
    private String status;
    private Integer total;

    public Long getBountyTaskId() {
        return bountyTaskId;
    }

    public void setBountyTaskId(Long bountyTaskId) {
        this.bountyTaskId = bountyTaskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
