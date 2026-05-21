package com.untitled.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {
    private String appid;
    private String secret;
    private String loginUrl;
    private Pay pay = new Pay();
    private Invite invite = new Invite();

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public Pay getPay() {
        return pay;
    }

    public void setPay(Pay pay) {
        this.pay = pay;
    }

    public Invite getInvite() {
        return invite;
    }

    public void setInvite(Invite invite) {
        this.invite = invite;
    }

    public static class Pay {
        private String mchid;
        private String spAppid;
        private String subMchid;
        private String subAppid;
        private String serialNo;
        private String privateKeyPath;
        private String apiV3Key;
        private String notifyUrl;
        private String withdrawNotifyUrl;
        private String apiBaseUrl;
        private String platformCertPath;
        private String transferSceneId;
        private String transferRemark;
        private String transferRecvPerception;
        private String transferInfoType;
        private String transferInfoContent;
        private String transferInfoType2;
        private String transferInfoContent2;

        public String getMchid() {
            return mchid;
        }

        public void setMchid(String mchid) {
            this.mchid = mchid;
        }

        public String getSpAppid() {
            return spAppid;
        }

        public void setSpAppid(String spAppid) {
            this.spAppid = spAppid;
        }

        public String getSubMchid() {
            return subMchid;
        }

        public void setSubMchid(String subMchid) {
            this.subMchid = subMchid;
        }

        public String getSubAppid() {
            return subAppid;
        }

        public void setSubAppid(String subAppid) {
            this.subAppid = subAppid;
        }

        public String getSerialNo() {
            return serialNo;
        }

        public void setSerialNo(String serialNo) {
            this.serialNo = serialNo;
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getApiV3Key() {
            return apiV3Key;
        }

        public void setApiV3Key(String apiV3Key) {
            this.apiV3Key = apiV3Key;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public void setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getPlatformCertPath() {
            return platformCertPath;
        }

        public void setPlatformCertPath(String platformCertPath) {
            this.platformCertPath = platformCertPath;
        }

        public String getWithdrawNotifyUrl() {
            return withdrawNotifyUrl;
        }

        public void setWithdrawNotifyUrl(String withdrawNotifyUrl) {
            this.withdrawNotifyUrl = withdrawNotifyUrl;
        }

        public String getTransferSceneId() {
            return transferSceneId;
        }

        public void setTransferSceneId(String transferSceneId) {
            this.transferSceneId = transferSceneId;
        }

        public String getTransferRemark() {
            return transferRemark;
        }

        public void setTransferRemark(String transferRemark) {
            this.transferRemark = transferRemark;
        }

        public String getTransferRecvPerception() {
            return transferRecvPerception;
        }

        public void setTransferRecvPerception(String transferRecvPerception) {
            this.transferRecvPerception = transferRecvPerception;
        }

        public String getTransferInfoType() {
            return transferInfoType;
        }

        public void setTransferInfoType(String transferInfoType) {
            this.transferInfoType = transferInfoType;
        }

        public String getTransferInfoContent() {
            return transferInfoContent;
        }

        public void setTransferInfoContent(String transferInfoContent) {
            this.transferInfoContent = transferInfoContent;
        }

        public String getTransferInfoType2() {
            return transferInfoType2;
        }

        public void setTransferInfoType2(String transferInfoType2) {
            this.transferInfoType2 = transferInfoType2;
        }

        public String getTransferInfoContent2() {
            return transferInfoContent2;
        }

        public void setTransferInfoContent2(String transferInfoContent2) {
            this.transferInfoContent2 = transferInfoContent2;
        }
    }

    public static class Invite {
        private String page = "pages/home/index";
        private String envVersion = "release";
        private Integer width = 430;
        private String fallbackUrl = "https://example.com/invite";

        public String getPage() {
            return page;
        }

        public void setPage(String page) {
            this.page = page;
        }

        public String getEnvVersion() {
            return envVersion;
        }

        public void setEnvVersion(String envVersion) {
            this.envVersion = envVersion;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public String getFallbackUrl() {
            return fallbackUrl;
        }

        public void setFallbackUrl(String fallbackUrl) {
            this.fallbackUrl = fallbackUrl;
        }
    }
}
