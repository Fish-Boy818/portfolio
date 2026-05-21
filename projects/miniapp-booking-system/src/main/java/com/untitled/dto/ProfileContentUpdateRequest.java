package com.untitled.dto;

import javax.validation.constraints.Size;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class ProfileContentUpdateRequest {
    @Size(max = 128)
    private String noticeTitle;

    @Size(max = 5000)
    private String noticeContent;

    @Size(max = 5000)
    private String aboutUsContent;

    @Size(max = 32)
    private String tabHomeText;

    @Size(max = 32)
    private String tabCategoryText;

    @Size(max = 32)
    private String tabOrdersText;

    @Size(max = 32)
    private String tabWelfareText;

    @Size(max = 32)
    private String tabProfileText;

    @Size(max = 32)
    private String platformServicePhone;

    @Min(0)
    @Max(1)
    private Integer reviewModeEnabled;

    @Min(1)
    @Max(50)
    private Integer siteActivityLimit;

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
}
