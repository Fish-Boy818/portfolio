package com.untitled.dto;

import java.time.LocalDateTime;

public class ProfileContentResponse {
    private Long id;
    private String noticeTitle;
    private String noticeContent;
    private String aboutUsContent;
    private String tabHomeText;
    private String tabCategoryText;
    private String tabOrdersText;
    private String tabWelfareText;
    private String tabProfileText;
    private String platformServicePhone;
    private Integer reviewModeEnabled;
    private Integer siteActivityLimit;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNoticeTitle() {
        return noticeTitle;
    }

    public void setNoticeTitle(String noticeTitle) {
        this.noticeTitle = noticeTitle;
    }

    public String getNoticeContent() {
        return noticeContent;
    }

    public void setNoticeContent(String noticeContent) {
        this.noticeContent = noticeContent;
    }

    public String getAboutUsContent() {
        return aboutUsContent;
    }

    public void setAboutUsContent(String aboutUsContent) {
        this.aboutUsContent = aboutUsContent;
    }

    public String getTabHomeText() {
        return tabHomeText;
    }

    public void setTabHomeText(String tabHomeText) {
        this.tabHomeText = tabHomeText;
    }

    public String getTabCategoryText() {
        return tabCategoryText;
    }

    public void setTabCategoryText(String tabCategoryText) {
        this.tabCategoryText = tabCategoryText;
    }

    public String getTabOrdersText() {
        return tabOrdersText;
    }

    public void setTabOrdersText(String tabOrdersText) {
        this.tabOrdersText = tabOrdersText;
    }

    public String getTabWelfareText() {
        return tabWelfareText;
    }

    public void setTabWelfareText(String tabWelfareText) {
        this.tabWelfareText = tabWelfareText;
    }

    public String getTabProfileText() {
        return tabProfileText;
    }

    public void setTabProfileText(String tabProfileText) {
        this.tabProfileText = tabProfileText;
    }

    public String getPlatformServicePhone() {
        return platformServicePhone;
    }

    public void setPlatformServicePhone(String platformServicePhone) {
        this.platformServicePhone = platformServicePhone;
    }

    public Integer getReviewModeEnabled() {
        return reviewModeEnabled;
    }

    public void setReviewModeEnabled(Integer reviewModeEnabled) {
        this.reviewModeEnabled = reviewModeEnabled;
    }

    public Integer getSiteActivityLimit() {
        return siteActivityLimit;
    }

    public void setSiteActivityLimit(Integer siteActivityLimit) {
        this.siteActivityLimit = siteActivityLimit;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
