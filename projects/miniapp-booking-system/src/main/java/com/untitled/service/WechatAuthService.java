package com.untitled.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.untitled.config.WechatProperties;
import com.untitled.dto.WechatSessionResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WechatAuthService {
    private static final String DEFAULT_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String DEFAULT_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String DEFAULT_PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final String DEFAULT_WXA_CODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit";

    private final WechatProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private volatile String cachedAccessToken;
    private volatile long accessTokenExpireAtEpochSecond;

    public WechatAuthService(WechatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public WechatSessionResponse getSession(String code) {
        if (StringUtils.hasText(code) && code.startsWith("dev-")) {
            WechatSessionResponse mock = new WechatSessionResponse();
            mock.setOpenid("dev-" + code);
            mock.setSessionKey("mock-session");
            return mock;
        }
        if (!StringUtils.hasText(properties.getAppid()) || !StringUtils.hasText(properties.getSecret())) {
            throw new IllegalArgumentException("请配置 wechat.appid 和 wechat.secret");
        }
        String loginUrl = StringUtils.hasText(properties.getLoginUrl())
                ? properties.getLoginUrl()
                : DEFAULT_LOGIN_URL;

        String url = UriComponentsBuilder.fromHttpUrl(loginUrl)
                .queryParam("appid", properties.getAppid())
                .queryParam("secret", properties.getSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        String payload = restTemplate.getForObject(url, String.class);
        WechatSessionResponse response = parseResponse(payload);
        if (response == null) {
            throw new IllegalArgumentException("微信登录失败");
        }
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            String message = StringUtils.hasText(response.getErrmsg()) ? response.getErrmsg() : "微信登录失败";
            throw new IllegalArgumentException("微信登录失败: " + message);
        }
        if (!StringUtils.hasText(response.getOpenid())) {
            throw new IllegalArgumentException("微信登录失败: 缺少 openid");
        }
        return response;
    }

    public String getPhoneNumberByCode(String phoneCode) {
        if (StringUtils.hasText(phoneCode) && phoneCode.startsWith("dev-")) {
            return "13800000000";
        }
        if (!StringUtils.hasText(phoneCode)) {
            throw new IllegalArgumentException("获取手机号失败: 缺少 phoneCode");
        }
        String accessToken = getAccessToken();
        String url = UriComponentsBuilder.fromHttpUrl(DEFAULT_PHONE_URL)
                .queryParam("access_token", accessToken)
                .toUriString();
        String payload = restTemplate.postForObject(
                url,
                Collections.singletonMap("code", phoneCode),
                String.class
        );
        WechatPhoneResponse response = parsePhoneResponse(payload);
        if (response == null) {
            throw new IllegalArgumentException("获取手机号失败");
        }
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            String message = StringUtils.hasText(response.getErrmsg()) ? response.getErrmsg() : "微信返回异常";
            throw new IllegalArgumentException("获取手机号失败: " + message);
        }
        WechatPhoneInfo phoneInfo = response.getPhoneInfo();
        if (phoneInfo == null) {
            throw new IllegalArgumentException("获取手机号失败: 缺少手机号信息");
        }
        if (StringUtils.hasText(phoneInfo.getPurePhoneNumber())) {
            return phoneInfo.getPurePhoneNumber();
        }
        if (StringUtils.hasText(phoneInfo.getPhoneNumber())) {
            return phoneInfo.getPhoneNumber();
        }
        throw new IllegalArgumentException("获取手机号失败: 手机号为空");
    }

    public byte[] getUnlimitedCode(String scene, String page, Integer width, String envVersion) {
        if (!StringUtils.hasText(scene)) {
            throw new IllegalArgumentException("生成二维码失败: scene 不能为空");
        }
        String accessToken = getAccessToken();
        String url = UriComponentsBuilder.fromHttpUrl(DEFAULT_WXA_CODE_URL)
                .queryParam("access_token", accessToken)
                .toUriString();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("scene", scene);
        body.put("page", StringUtils.hasText(page) ? page : "pages/home/index");
        body.put("check_path", false);
        body.put("env_version", StringUtils.hasText(envVersion) ? envVersion : "release");
        if (width != null) {
            body.put("width", width);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        byte[] data = response.getBody();
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("生成二维码失败: 微信返回为空");
        }
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null && MediaType.APPLICATION_JSON.includes(contentType)) {
            String payload = new String(data, StandardCharsets.UTF_8);
            WechatSessionResponse err = parseResponse(payload);
            if (err != null && err.getErrcode() != null && err.getErrcode() != 0) {
                String message = StringUtils.hasText(err.getErrmsg()) ? err.getErrmsg() : "微信返回异常";
                throw new IllegalArgumentException("生成二维码失败: " + message);
            }
            throw new IllegalArgumentException("生成二维码失败");
        }
        return data;
    }

    private String getAccessToken() {
        long now = Instant.now().getEpochSecond();
        String token = cachedAccessToken;
        if (StringUtils.hasText(token) && now < accessTokenExpireAtEpochSecond) {
            return token;
        }
        synchronized (this) {
            now = Instant.now().getEpochSecond();
            token = cachedAccessToken;
            if (StringUtils.hasText(token) && now < accessTokenExpireAtEpochSecond) {
                return token;
            }
            AccessTokenResponse response = requestAccessToken();
            if (response.getErrcode() != null && response.getErrcode() != 0) {
                String message = StringUtils.hasText(response.getErrmsg()) ? response.getErrmsg() : "微信返回异常";
                throw new IllegalArgumentException("获取微信 access_token 失败: " + message);
            }
            if (!StringUtils.hasText(response.getAccessToken())) {
                throw new IllegalArgumentException("获取微信 access_token 失败: 响应缺少 access_token");
            }
            int expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 7200;
            long ttl = Math.max(60, expiresIn - 60);
            cachedAccessToken = response.getAccessToken();
            accessTokenExpireAtEpochSecond = Instant.now().getEpochSecond() + ttl;
            return cachedAccessToken;
        }
    }

    private AccessTokenResponse requestAccessToken() {
        if (!StringUtils.hasText(properties.getAppid()) || !StringUtils.hasText(properties.getSecret())) {
            throw new IllegalArgumentException("请配置 wechat.appid 和 wechat.secret");
        }
        String url = UriComponentsBuilder.fromHttpUrl(DEFAULT_ACCESS_TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", properties.getAppid())
                .queryParam("secret", properties.getSecret())
                .toUriString();
        String payload = restTemplate.getForObject(url, String.class);
        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("获取微信 access_token 失败: 响应为空");
        }
        try {
            return objectMapper.readValue(payload, AccessTokenResponse.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("获取微信 access_token 失败: 响应解析异常");
        }
    }

    private WechatSessionResponse parseResponse(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, WechatSessionResponse.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("微信登录失败: 响应解析异常");
        }
    }

    private WechatPhoneResponse parsePhoneResponse(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, WechatPhoneResponse.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("获取手机号失败: 响应解析异常");
        }
    }

    private static class AccessTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        private Integer errcode;
        private String errmsg;

        public String getAccessToken() {
            return accessToken;
        }

        public Integer getExpiresIn() {
            return expiresIn;
        }

        public Integer getErrcode() {
            return errcode;
        }

        public String getErrmsg() {
            return errmsg;
        }
    }

    private static class WechatPhoneResponse {
        private Integer errcode;
        private String errmsg;
        @JsonProperty("phone_info")
        private WechatPhoneInfo phoneInfo;

        public Integer getErrcode() {
            return errcode;
        }

        public String getErrmsg() {
            return errmsg;
        }

        public WechatPhoneInfo getPhoneInfo() {
            return phoneInfo;
        }
    }

    private static class WechatPhoneInfo {
        @JsonProperty("phoneNumber")
        private String phoneNumber;
        @JsonProperty("purePhoneNumber")
        private String purePhoneNumber;
        @JsonProperty("countryCode")
        private String countryCode;

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getPurePhoneNumber() {
            return purePhoneNumber;
        }

        public String getCountryCode() {
            return countryCode;
        }
    }
}
