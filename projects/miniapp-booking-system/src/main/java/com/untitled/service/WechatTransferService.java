package com.untitled.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.untitled.config.WechatProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class WechatTransferService {
    private static final String AUTH_SCHEME = "WECHATPAY2-SHA256-RSA2048";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final String DEFAULT_API_BASE_URL = "https://api.mch.weixin.qq.com";

    private final WechatProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    public WechatTransferService(WechatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TransferCreateResult createTransferBill(String outBillNo, String openId, int amountFen) {
        checkConfig();
        if (!StringUtils.hasText(outBillNo)) {
            throw new IllegalArgumentException("提现单号不能为空");
        }
        if (!StringUtils.hasText(openId)) {
            throw new IllegalArgumentException("用户缺少 openId，请重新登录");
        }
        if (amountFen <= 0) {
            throw new IllegalArgumentException("提现金额不合法");
        }
        String endpoint = "/v3/fund-app/mch-transfer/transfer-bills";
        String url = trimTrailingSlash(resolveApiBaseUrl()) + endpoint;

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("appid", resolvePayAppId());
        body.put("out_bill_no", outBillNo);
        body.put("transfer_scene_id", properties.getPay().getTransferSceneId().trim());
        body.put("openid", openId);
        body.put("transfer_amount", amountFen);
        body.put("transfer_remark", trimToDefault(properties.getPay().getTransferRemark(), "佣金提现"));
        body.put("user_recv_perception", trimToDefault(properties.getPay().getTransferRecvPerception(), "佣金到账"));
        body.put("notify_url", resolveWithdrawNotifyUrl());
        List<Map<String, String>> reports = new ArrayList<Map<String, String>>();
        reports.add(buildReportInfo(
                properties.getPay().getTransferInfoType(),
                properties.getPay().getTransferInfoContent()
        ));
        if (StringUtils.hasText(properties.getPay().getTransferInfoType2())
                && StringUtils.hasText(properties.getPay().getTransferInfoContent2())) {
            reports.add(buildReportInfo(
                    properties.getPay().getTransferInfoType2(),
                    properties.getPay().getTransferInfoContent2()
            ));
        }
        body.put("transfer_scene_report_infos", reports);

        String responseBody;
        try {
            responseBody = signedRequest(HttpMethod.POST, url, writeJson(body));
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            if (!StringUtils.hasText(detail)) {
                detail = ex.getMessage();
            }
            throw new IllegalArgumentException("调用微信提现失败: " + detail);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("调用微信提现失败: " + ex.getMessage());
        }
        JsonNode node = readJson(responseBody);
        String code = text(node, "code");
        if (StringUtils.hasText(code)) {
            String message = text(node, "message");
            throw new IllegalArgumentException("调用微信提现失败: " + code + " " + message);
        }
        TransferCreateResult result = new TransferCreateResult();
        result.setOutBillNo(outBillNo);
        result.setTransferBillNo(text(node, "transfer_bill_no"));
        result.setState(text(node, "state"));
        result.setPackageInfo(text(node, "package_info"));
        result.setFailReason(text(node, "fail_reason"));
        return result;
    }

    public TransferQueryResult queryTransferBill(String outBillNo) {
        checkConfig();
        if (!StringUtils.hasText(outBillNo)) {
            throw new IllegalArgumentException("提现单号不能为空");
        }
        String endpoint = "/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/" + outBillNo;
        String url = trimTrailingSlash(resolveApiBaseUrl()) + endpoint;
        String responseBody;
        try {
            responseBody = signedRequest(HttpMethod.GET, url, "");
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            if (!StringUtils.hasText(detail)) {
                detail = ex.getMessage();
            }
            throw new IllegalArgumentException("查询微信提现状态失败: " + detail);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("查询微信提现状态失败: " + ex.getMessage());
        }
        JsonNode node = readJson(responseBody);
        String code = text(node, "code");
        if (StringUtils.hasText(code)) {
            String message = text(node, "message");
            throw new IllegalArgumentException("查询微信提现状态失败: " + code + " " + message);
        }
        TransferQueryResult result = new TransferQueryResult();
        result.setOutBillNo(text(node, "out_bill_no"));
        result.setTransferBillNo(text(node, "transfer_bill_no"));
        result.setState(text(node, "state"));
        result.setFailReason(text(node, "fail_reason"));
        return result;
    }

    public TransferNotifyResult parseTransferNotify(String payload,
                                                    String timestamp,
                                                    String nonce,
                                                    String signature,
                                                    String serial) {
        verifyNotifySignature(payload, timestamp, nonce, signature, serial);
        JsonNode root = readJson(payload);
        JsonNode resource = root.get("resource");
        if (resource == null || resource.isNull()) {
            throw new IllegalArgumentException("resource missing");
        }
        String plain = decryptResource(resource);
        JsonNode node = readJson(plain);
        TransferNotifyResult result = new TransferNotifyResult();
        result.setOutBillNo(text(node, "out_bill_no"));
        result.setTransferBillNo(text(node, "transfer_bill_no"));
        result.setState(text(node, "state"));
        result.setFailReason(text(node, "fail_reason"));
        return result;
    }

    public Map<String, String> successResponse() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("code", SUCCESS);
        result.put("message", "成功");
        return result;
    }

    public Map<String, String> failResponse(String message) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("code", FAIL);
        result.put("message", message);
        return result;
    }

    public String resolveMchId() {
        return trimToDefault(properties.getPay().getMchid(), "");
    }

    public String resolvePayAppId() {
        String appid = trimToDefault(properties.getAppid(), "");
        if (StringUtils.hasText(appid)) {
            return appid;
        }
        throw new IllegalArgumentException("WECHAT_APPID 未配置");
    }

    private void checkConfig() {
        if (!StringUtils.hasText(resolvePayAppId())) {
            throw new IllegalArgumentException("WECHAT_APPID 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getMchid())) {
            throw new IllegalArgumentException("WECHAT_PAY_MCHID 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getSerialNo())) {
            throw new IllegalArgumentException("WECHAT_PAY_SERIAL_NO 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getPrivateKeyPath())) {
            throw new IllegalArgumentException("WECHAT_PAY_PRIVATE_KEY_PATH 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getApiV3Key())) {
            throw new IllegalArgumentException("WECHAT_PAY_API_V3_KEY 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getTransferSceneId())) {
            throw new IllegalArgumentException("WECHAT_PAY_TRANSFER_SCENE_ID 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getTransferInfoType())) {
            throw new IllegalArgumentException("WECHAT_PAY_TRANSFER_INFO_TYPE 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getTransferInfoContent())) {
            throw new IllegalArgumentException("WECHAT_PAY_TRANSFER_INFO_CONTENT 未配置");
        }
        if ("1005".equals(trimToDefault(properties.getPay().getTransferSceneId(), ""))) {
            if (!StringUtils.hasText(properties.getPay().getTransferInfoType2())) {
                throw new IllegalArgumentException("佣金报酬场景需配置 WECHAT_PAY_TRANSFER_INFO_TYPE2");
            }
            if (!StringUtils.hasText(properties.getPay().getTransferInfoContent2())) {
                throw new IllegalArgumentException("佣金报酬场景需配置 WECHAT_PAY_TRANSFER_INFO_CONTENT2");
            }
        }
        if (!StringUtils.hasText(resolveWithdrawNotifyUrl())) {
            throw new IllegalArgumentException("WECHAT_PAY_WITHDRAW_NOTIFY_URL 未配置");
        }
    }

    private Map<String, String> buildReportInfo(String type, String content) {
        Map<String, String> report = new HashMap<String, String>();
        report.put("info_type", type.trim());
        report.put("info_content", content.trim());
        return report;
    }

    private String resolveWithdrawNotifyUrl() {
        if (StringUtils.hasText(properties.getPay().getWithdrawNotifyUrl())) {
            return properties.getPay().getWithdrawNotifyUrl().trim();
        }
        if (StringUtils.hasText(properties.getPay().getNotifyUrl())) {
            return properties.getPay().getNotifyUrl().trim();
        }
        return "";
    }

    private String resolveApiBaseUrl() {
        if (StringUtils.hasText(properties.getPay().getApiBaseUrl())) {
            return properties.getPay().getApiBaseUrl().trim();
        }
        return DEFAULT_API_BASE_URL;
    }

    private String signedRequest(HttpMethod method, String url, String body) {
        URI uri = URI.create(url);
        String canonicalUrl = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            canonicalUrl = canonicalUrl + "?" + uri.getRawQuery();
        }
        String nonce = randomString(32);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String content = body == null ? "" : body;
        String message = method.name() + "\n" + canonicalUrl + "\n" + timestamp + "\n" + nonce + "\n" + content + "\n";
        String signature = sign(message);

        String authorization = AUTH_SCHEME
                + " mchid=\"" + properties.getPay().getMchid() + "\""
                + ",nonce_str=\"" + nonce + "\""
                + ",signature=\"" + signature + "\""
                + ",timestamp=\"" + timestamp + "\""
                + ",serial_no=\"" + properties.getPay().getSerialNo() + "\"";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", authorization);
        headers.set("User-Agent", "untitled/1.0");
        HttpEntity<String> entity;
        if (HttpMethod.POST.equals(method)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            entity = new HttpEntity<String>(content, headers);
        } else {
            entity = new HttpEntity<String>(headers);
        }
        ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
        return response.getBody();
    }

    private String decryptResource(JsonNode resource) {
        try {
            String algorithm = text(resource, "algorithm");
            if (!"AEAD_AES_256_GCM".equals(algorithm)) {
                throw new IllegalArgumentException("unsupported algorithm");
            }
            String nonce = text(resource, "nonce");
            String ciphertext = text(resource, "ciphertext");
            String associatedData = text(resource, "associated_data");
            String apiV3Key = properties.getPay().getApiV3Key();
            if (!StringUtils.hasText(apiV3Key) || apiV3Key.length() != 32) {
                throw new IllegalArgumentException("WECHAT_PAY_API_V3_KEY 必须是32位");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcm = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcm);
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] plain = cipher.doFinal(decoded);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("回调解密失败: " + ex.getMessage());
        }
    }

    private void verifyNotifySignature(String payload,
                                       String timestamp,
                                       String nonce,
                                       String signature,
                                       String serial) {
        String certPath = properties.getPay().getPlatformCertPath();
        if (!StringUtils.hasText(certPath)) {
            return;
        }
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            throw new IllegalArgumentException("微信提现回调签名头缺失");
        }
        try {
            PublicKey publicKey = loadPlatformPublicKey(certPath);
            String message = timestamp + "\n" + nonce + "\n" + payload + "\n";
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            boolean ok = verifier.verify(Base64.getDecoder().decode(signature));
            if (!ok) {
                throw new IllegalArgumentException("signature verify failed");
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("微信提现回调签名校验失败: " + ex.getMessage());
        }
    }

    private PublicKey loadPlatformPublicKey(String certPath) {
        try {
            byte[] content = Files.readAllBytes(Paths.get(certPath));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(content));
            return certificate.getPublicKey();
        } catch (Exception ex) {
            throw new IllegalArgumentException("读取平台证书失败: " + ex.getMessage());
        }
    }

    private String sign(String text) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey());
            signature.update(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new IllegalArgumentException("微信提现签名失败: " + ex.getMessage());
        }
    }

    private PrivateKey loadPrivateKey() {
        try {
            String path = properties.getPay().getPrivateKeyPath();
            if (!StringUtils.hasText(path)) {
                throw new IllegalArgumentException("WECHAT_PAY_PRIVATE_KEY_PATH 未配置");
            }
            byte[] content = Files.readAllBytes(Paths.get(path));
            String pem = new String(content, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception ex) {
            throw new IllegalArgumentException("读取商户私钥失败: " + ex.getMessage());
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON序列化失败");
        }
    }

    private JsonNode readJson(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON解析失败");
        }
    }

    private String text(JsonNode node, String key) {
        if (node == null || node.get(key) == null || node.get(key).isNull()) {
            return "";
        }
        return node.get(key).asText("");
    }

    private String trimToDefault(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String randomString(int len) {
        String dict = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(dict.charAt(random.nextInt(dict.length())));
        }
        return sb.toString();
    }

    public static class TransferCreateResult {
        private String outBillNo;
        private String transferBillNo;
        private String state;
        private String packageInfo;
        private String failReason;

        public String getOutBillNo() {
            return outBillNo;
        }

        public void setOutBillNo(String outBillNo) {
            this.outBillNo = outBillNo;
        }

        public String getTransferBillNo() {
            return transferBillNo;
        }

        public void setTransferBillNo(String transferBillNo) {
            this.transferBillNo = transferBillNo;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPackageInfo() {
            return packageInfo;
        }

        public void setPackageInfo(String packageInfo) {
            this.packageInfo = packageInfo;
        }

        public String getFailReason() {
            return failReason;
        }

        public void setFailReason(String failReason) {
            this.failReason = failReason;
        }
    }

    public static class TransferQueryResult {
        private String outBillNo;
        private String transferBillNo;
        private String state;
        private String failReason;

        public String getOutBillNo() {
            return outBillNo;
        }

        public void setOutBillNo(String outBillNo) {
            this.outBillNo = outBillNo;
        }

        public String getTransferBillNo() {
            return transferBillNo;
        }

        public void setTransferBillNo(String transferBillNo) {
            this.transferBillNo = transferBillNo;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getFailReason() {
            return failReason;
        }

        public void setFailReason(String failReason) {
            this.failReason = failReason;
        }
    }

    public static class TransferNotifyResult {
        private String outBillNo;
        private String transferBillNo;
        private String state;
        private String failReason;

        public String getOutBillNo() {
            return outBillNo;
        }

        public void setOutBillNo(String outBillNo) {
            this.outBillNo = outBillNo;
        }

        public String getTransferBillNo() {
            return transferBillNo;
        }

        public void setTransferBillNo(String transferBillNo) {
            this.transferBillNo = transferBillNo;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getFailReason() {
            return failReason;
        }

        public void setFailReason(String failReason) {
            this.failReason = failReason;
        }
    }
}
