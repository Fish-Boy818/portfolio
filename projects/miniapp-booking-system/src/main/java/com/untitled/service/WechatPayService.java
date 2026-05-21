package com.untitled.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.untitled.config.WechatProperties;
import com.untitled.dto.OrderResponse;
import com.untitled.dto.WechatPayParamsResponse;
import com.untitled.mapper.OrderMapper;
import com.untitled.mapper.UserMapper;
import com.untitled.model.Order;
import com.untitled.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Optional;

@Service
public class WechatPayService {
    private static final Logger log = LoggerFactory.getLogger(WechatPayService.class);
    private static final String SIGN_TYPE = "RSA";
    private static final String AUTH_SCHEME = "WECHATPAY2-SHA256-RSA2048";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final int MAX_WECHAT_DESCRIPTION_BYTES = 127;

    private final WechatProperties properties;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    public WechatPayService(WechatProperties properties,
                            OrderMapper orderMapper,
                            UserMapper userMapper,
                            OrderService orderService,
                            ObjectMapper objectMapper) {
        this.properties = properties;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    public WechatPayParamsResponse createJsapiPay(long orderId) {
        checkPayConfig();
        orderService.cancelIfExpired(orderId);
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!"unpaid".equals(order.getStatus())) {
            if ("cancelled".equals(order.getStatus())) {
                throw new IllegalArgumentException("订单已超时取消，请重新下单");
            }
            throw new IllegalArgumentException("订单状态不可支付");
        }
        User user = userMapper.findById(order.getUserId());
        if (user == null || !StringUtils.hasText(user.getOpenId())) {
            throw new IllegalArgumentException("用户缺少微信 openId，请重新登录");
        }

        boolean partnerMode = isPartnerMode();
        String endpoint = partnerMode ? "/v3/pay/partner/transactions/jsapi" : "/v3/pay/transactions/jsapi";
        String apiBase = StringUtils.hasText(properties.getPay().getApiBaseUrl())
                ? properties.getPay().getApiBaseUrl().trim()
                : "https://api.mch.weixin.qq.com";
        String url = trimTrailingSlash(apiBase) + endpoint;
        String payAppId = resolvePayAppId(partnerMode);

        Map<String, Object> amount = new HashMap<String, Object>();
        amount.put("total", toFen(order.getPayAmount()));
        amount.put("currency", "CNY");

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("description", truncate(order.getTitle(), MAX_WECHAT_DESCRIPTION_BYTES));
        body.put("out_trade_no", order.getOrderNo());
        body.put("notify_url", properties.getPay().getNotifyUrl());
        body.put("amount", amount);
        if (partnerMode) {
            Map<String, Object> payer = new HashMap<String, Object>();
            String subAppid = trimToEmpty(properties.getPay().getSubAppid());
            if (StringUtils.hasText(subAppid)) {
                payer.put("sub_openid", user.getOpenId());
                body.put("sub_appid", subAppid);
            } else {
                payer.put("sp_openid", user.getOpenId());
            }
            body.put("sp_appid", resolveSpAppId());
            body.put("sp_mchid", trimToEmpty(properties.getPay().getMchid()));
            body.put("sub_mchid", trimToEmpty(properties.getPay().getSubMchid()));
            body.put("payer", payer);
        } else {
            Map<String, Object> payer = new HashMap<String, Object>();
            payer.put("openid", user.getOpenId());
            body.put("appid", payAppId);
            body.put("mchid", properties.getPay().getMchid());
            body.put("payer", payer);
        }

        String requestBody = writeJson(body);
        String responseBody;
        try {
            responseBody = signedPost(url, requestBody);
        } catch (RestClientResponseException ex) {
            String detail = responseDetail(ex);
            String userMessage = wechatPayUserMessage(detail);
            log.error("wechat jsapi pay failed, orderId={}, orderNo={}, status={}, body={}",
                    orderId,
                    order.getOrderNo(),
                    ex.getRawStatusCode(),
                    detail);
            throw new IllegalArgumentException("微信支付下单失败：" + userMessage);
        } catch (RestClientException ex) {
            log.error("wechat jsapi pay request failed, orderId={}, orderNo={}", orderId, order.getOrderNo(), ex);
            throw new IllegalArgumentException("微信支付下单失败：网络异常，请稍后重试或联系商家");
        }

        JsonNode node = readJson(responseBody);
        String prepayId = text(node, "prepay_id");
        if (!StringUtils.hasText(prepayId)) {
            String code = text(node, "code");
            String message = text(node, "message");
            if (StringUtils.hasText(code) || StringUtils.hasText(message)) {
                throw new IllegalArgumentException("微信下单失败: " + code + " " + message);
            }
            throw new IllegalArgumentException("微信下单失败: 未返回 prepay_id");
        }
        return buildMiniPayParams(prepayId, payAppId);
    }

    public OrderResponse confirmPaid(long orderId) {
        checkPayConfig();
        orderService.cancelIfExpired(orderId);
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if ("cancelled".equals(order.getStatus())) {
            throw new IllegalArgumentException("订单已超时取消，请重新下单");
        }
        if (!"unpaid".equals(order.getStatus())) {
            return orderService.get(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        }

        JsonNode result = queryTransaction(order);
        String tradeState = text(result, "trade_state");
        String transactionId = text(result, "transaction_id");
        JsonNode amountNode = result.get("amount");
        if (amountNode != null && !amountNode.isNull() && order.getPayAmount() != null) {
            int expected = toFen(order.getPayAmount());
            int actual = amountNode.path("total").asInt(-1);
            if (actual > -1 && actual != expected) {
                log.error("confirm paid amount mismatch, orderNo={}, expected={}, actual={}", order.getOrderNo(), expected, actual);
                throw new IllegalArgumentException("支付金额校验失败");
            }
        }

        if (SUCCESS.equals(tradeState)) {
            OrderResponse response = orderService.pay(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
            log.info("confirm paid success, orderNo={}, transactionId={}", order.getOrderNo(), transactionId);
            return response;
        }
        if ("USERPAYING".equals(tradeState)) {
            throw new IllegalArgumentException("支付结果确认中，请稍后重试");
        }
        if ("NOTPAY".equals(tradeState)) {
            throw new IllegalArgumentException("支付尚未完成");
        }
        if ("CLOSED".equals(tradeState)) {
            throw new IllegalArgumentException("微信订单已关闭");
        }
        if ("REVOKED".equals(tradeState)) {
            throw new IllegalArgumentException("订单已撤销支付");
        }
        if ("PAYERROR".equals(tradeState)) {
            throw new IllegalArgumentException("支付失败，请重新发起支付");
        }
        throw new IllegalArgumentException("支付状态异常: " + (StringUtils.hasText(tradeState) ? tradeState : "未知"));
    }

    public OrderResponse createRefund(long orderId, boolean reclaimCommission) {
        checkPayConfig();
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!"paid".equals(order.getStatus()) && !"used".equals(order.getStatus())) {
            throw new IllegalArgumentException("仅已支付或已核销订单可退款");
        }
        boolean finalReclaimCommission = reclaimCommission;
        if (finalReclaimCommission) {
            try {
                orderService.assertRefundAllowed(orderId);
            } catch (IllegalArgumentException ex) {
                if (shouldSkipCommissionReclaim(ex)) {
                    finalReclaimCommission = false;
                    log.warn("order refund fallback to no-commission-reclaim, orderId={}, reason={}", orderId, ex.getMessage());
                } else {
                    throw ex;
                }
            }
        }
        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额不合法");
        }

        String apiBase = StringUtils.hasText(properties.getPay().getApiBaseUrl())
                ? properties.getPay().getApiBaseUrl().trim()
                : "https://api.mch.weixin.qq.com";
        String url = trimTrailingSlash(apiBase) + "/v3/refund/domestic/refunds";

        Map<String, Object> amount = new HashMap<String, Object>();
        int total = toFen(order.getPayAmount());
        amount.put("refund", total);
        amount.put("total", total);
        amount.put("currency", "CNY");

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("out_trade_no", order.getOrderNo());
        body.put("out_refund_no", buildOutRefundNo(order.getOrderNo(), finalReclaimCommission));
        body.put("reason", finalReclaimCommission ? "用户申请退款" : "管理端操作退款");
        body.put("amount", amount);
        if (StringUtils.hasText(properties.getPay().getNotifyUrl())) {
            body.put("notify_url", properties.getPay().getNotifyUrl());
        }
        if (isPartnerMode()) {
            body.put("sub_mchid", trimToEmpty(properties.getPay().getSubMchid()));
        }

        String requestBody = writeJson(body);
        String responseBody;
        try {
            responseBody = signedPost(url, requestBody);
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            if (!StringUtils.hasText(detail)) {
                detail = ex.getMessage();
            }
            throw new IllegalArgumentException("调用微信退款失败: " + detail);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("调用微信退款失败: " + ex.getMessage());
        }

        JsonNode node = readJson(responseBody);
        String code = text(node, "code");
        String message = text(node, "message");
        if (StringUtils.hasText(code)) {
            throw new IllegalArgumentException("微信退款失败: " + code + " " + message);
        }
        String refundStatus = text(node, "status");
        if (!StringUtils.hasText(refundStatus)) {
            throw new IllegalArgumentException("微信退款失败: 未返回退款状态");
        }
        if (!"SUCCESS".equals(refundStatus) && !"PROCESSING".equals(refundStatus)) {
            throw new IllegalArgumentException("微信退款失败: " + refundStatus + " " + message);
        }

        orderService.refund(orderId, finalReclaimCommission, finalReclaimCommission ? "用户退款冲销佣金" : null)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        Optional<OrderResponse> updated = orderService.get(orderId);
        if (!updated.isPresent()) {
            throw new IllegalArgumentException("订单不存在");
        }
        return updated.get();
    }

    public Map<String, String> handleNotify(String payload,
                                            String timestamp,
                                            String nonce,
                                            String signature,
                                            String serial) {
        try {
            verifyNotifySignature(payload, timestamp, nonce, signature, serial);
            JsonNode root = readJson(payload);
            JsonNode resource = root.get("resource");
            if (resource == null || resource.isNull()) {
                return failResponse("resource missing");
            }
            String plainText = decryptResource(resource);
            JsonNode result = readJson(plainText);
            if (isRefundNotify(root, result)) {
                return handleRefundNotify(result);
            }
            return handleTransactionNotify(result);
        } catch (Exception ex) {
            log.error("pay notify handle error", ex);
            return failResponse("notify handle error");
        }
    }

    private Map<String, String> handleTransactionNotify(JsonNode result) {
        String outTradeNo = text(result, "out_trade_no");
        String tradeState = text(result, "trade_state");
        String transactionId = text(result, "transaction_id");
        if (!StringUtils.hasText(outTradeNo)) {
            return failResponse("out_trade_no missing");
        }
        Order order = orderMapper.findByOrderNo(outTradeNo);
        if (order == null) {
            log.warn("notify order not found, outTradeNo={}", outTradeNo);
            return successResponse();
        }

        JsonNode amountNode = result.get("amount");
        if (amountNode != null && !amountNode.isNull() && order.getPayAmount() != null) {
            int expected = toFen(order.getPayAmount());
            int actual = amountNode.path("total").asInt(-1);
            if (actual > -1 && actual != expected) {
                log.error("notify amount mismatch, orderNo={}, expected={}, actual={}", outTradeNo, expected, actual);
                return failResponse("amount mismatch");
            }
        }

        if (SUCCESS.equals(tradeState) && "unpaid".equals(order.getStatus())) {
            orderService.pay(order.getId());
            log.info("notify paid success, orderNo={}, transactionId={}", outTradeNo, transactionId);
        }
        return successResponse();
    }

    private Map<String, String> handleRefundNotify(JsonNode result) {
        String outTradeNo = text(result, "out_trade_no");
        String outRefundNo = text(result, "out_refund_no");
        String refundStatus = text(result, "refund_status");
        if (!StringUtils.hasText(outTradeNo)) {
            return failResponse("refund out_trade_no missing");
        }
        Order order = orderMapper.findByOrderNo(outTradeNo);
        if (order == null) {
            log.warn("refund notify order not found, outTradeNo={}, outRefundNo={}", outTradeNo, outRefundNo);
            return successResponse();
        }
        if ("SUCCESS".equals(refundStatus)) {
            if (!"refund".equals(order.getStatus())) {
                boolean reclaimCommission = resolveRefundShouldReclaimCommission(outRefundNo);
                orderService.refund(order.getId(),
                        reclaimCommission,
                        reclaimCommission ? "用户退款冲销佣金" : null);
            }
            log.info("notify refund success, orderNo={}, outRefundNo={}", outTradeNo, outRefundNo);
            return successResponse();
        }
        if ("PROCESSING".equals(refundStatus)) {
            log.info("notify refund processing, orderNo={}, outRefundNo={}", outTradeNo, outRefundNo);
            return successResponse();
        }
        log.warn("notify refund not-success, orderNo={}, outRefundNo={}, status={}", outTradeNo, outRefundNo, refundStatus);
        return successResponse();
    }

    private boolean isRefundNotify(JsonNode root, JsonNode result) {
        String eventType = text(root, "event_type");
        if (eventType != null && eventType.startsWith("REFUND")) {
            return true;
        }
        return StringUtils.hasText(text(result, "refund_status")) || StringUtils.hasText(text(result, "out_refund_no"));
    }

    private String signedPost(String url, String body) {
        URI uri = URI.create(url);
        String canonicalUrl = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            canonicalUrl = canonicalUrl + "?" + uri.getRawQuery();
        }
        String nonce = randomString(32);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String message = "POST\n" + canonicalUrl + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = sign(message);

        String authorization = AUTH_SCHEME
                + " mchid=\"" + properties.getPay().getMchid() + "\""
                + ",nonce_str=\"" + nonce + "\""
                + ",signature=\"" + signature + "\""
                + ",timestamp=\"" + timestamp + "\""
                + ",serial_no=\"" + properties.getPay().getSerialNo() + "\"";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", authorization);
        headers.set("User-Agent", "untitled/1.0");

        HttpEntity<String> entity = new HttpEntity<String>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    private JsonNode queryTransaction(Order order) {
        String apiBase = StringUtils.hasText(properties.getPay().getApiBaseUrl())
                ? properties.getPay().getApiBaseUrl().trim()
                : "https://api.mch.weixin.qq.com";
        boolean partnerMode = isPartnerMode();
        String encodedOrderNo = order.getOrderNo();
        String url;
        if (partnerMode) {
            url = trimTrailingSlash(apiBase)
                    + "/v3/pay/partner/transactions/out-trade-no/"
                    + encodedOrderNo
                    + "?sp_mchid=" + trimToEmpty(properties.getPay().getMchid())
                    + "&sub_mchid=" + trimToEmpty(properties.getPay().getSubMchid());
        } else {
            url = trimTrailingSlash(apiBase)
                    + "/v3/pay/transactions/out-trade-no/"
                    + encodedOrderNo
                    + "?mchid=" + trimToEmpty(properties.getPay().getMchid());
        }
        String responseBody;
        try {
            responseBody = signedGet(url);
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            if (!StringUtils.hasText(detail)) {
                detail = ex.getMessage();
            }
            throw new IllegalArgumentException("查询微信支付结果失败: " + detail);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("查询微信支付结果失败: " + ex.getMessage());
        }
        JsonNode node = readJson(responseBody);
        String code = text(node, "code");
        String message = text(node, "message");
        if (StringUtils.hasText(code)) {
            throw new IllegalArgumentException("查询微信支付结果失败: " + code + " " + message);
        }
        return node;
    }

    private String signedGet(String url) {
        URI uri = URI.create(url);
        String canonicalUrl = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            canonicalUrl = canonicalUrl + "?" + uri.getRawQuery();
        }
        String nonce = randomString(32);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String message = "GET\n" + canonicalUrl + "\n" + timestamp + "\n" + nonce + "\n\n";
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

        HttpEntity<Void> entity = new HttpEntity<Void>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private WechatPayParamsResponse buildMiniPayParams(String prepayId, String appId) {
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = randomString(32);
        String packageValue = "prepay_id=" + prepayId;
        String toSign = appId + "\n"
                + timeStamp + "\n"
                + nonceStr + "\n"
                + packageValue + "\n";
        String paySign = sign(toSign);

        WechatPayParamsResponse response = new WechatPayParamsResponse();
        response.setAppId(appId);
        response.setTimeStamp(timeStamp);
        response.setNonceStr(nonceStr);
        response.setPackageValue(packageValue);
        response.setSignType(SIGN_TYPE);
        response.setPaySign(paySign);
        return response;
    }

    private String decryptResource(JsonNode resource) throws Exception {
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
    }

    private String sign(String text) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey());
            signature.update(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new IllegalArgumentException("签名失败: " + ex.getMessage());
        }
    }

    private void verifyNotifySignature(String payload,
                                       String timestamp,
                                       String nonce,
                                       String signature,
                                       String serial) {
        String certPath = properties.getPay().getPlatformCertPath();
        if (!StringUtils.hasText(certPath)) {
            throw new IllegalArgumentException("WECHAT_PAY_PLATFORM_CERT_PATH 未配置，拒绝处理回调");
        }
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            throw new IllegalArgumentException("微信回调签名头缺失");
        }
        try {
            PublicKey publicKey = loadPlatformPublicKey(certPath);
            String message = timestamp + "\n" + nonce + "\n" + payload + "\n";
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            boolean ok = verifier.verify(Base64.getDecoder().decode(signature));
            if (!ok) {
                throw new IllegalArgumentException("微信回调签名校验失败");
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("微信回调签名校验失败: " + ex.getMessage());
        }
    }

    private PublicKey loadPlatformPublicKey(String certPath) {
        try {
            byte[] content = Files.readAllBytes(Paths.get(certPath));
            String text = new String(content, StandardCharsets.UTF_8).trim();
            if (text.contains("BEGIN CERTIFICATE")) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(content));
                return certificate.getPublicKey();
            }
            if (text.contains("BEGIN PUBLIC KEY")) {
                String pem = text
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
                byte[] decoded = Base64.getDecoder().decode(pem);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(keySpec);
            }
            if (text.contains("BEGIN RSA PUBLIC KEY")) {
                throw new IllegalArgumentException("平台公钥格式不支持 BEGIN RSA PUBLIC KEY，请下载/转换为 BEGIN PUBLIC KEY");
            }
            throw new IllegalArgumentException("平台证书/公钥文件格式不正确");
        } catch (Exception ex) {
            throw new IllegalArgumentException("读取平台证书/公钥失败: " + ex.getMessage());
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

    private int toFen(BigDecimal amount) {
        if (amount == null) {
            return 0;
        }
        return amount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String truncate(String text, int maxBytes) {
        if (!StringUtils.hasText(text)) {
            return "海上活动订单";
        }
        String value = text.trim();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            String ch = new String(Character.toChars(codePoint));
            int len = ch.getBytes(StandardCharsets.UTF_8).length;
            if (used + len > maxBytes) {
                break;
            }
            sb.append(ch);
            used += len;
            i += Character.charCount(codePoint);
        }
        return sb.length() > 0 ? sb.toString() : "海上活动订单";
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

    private String responseDetail(RestClientResponseException ex) {
        String detail = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        if (!StringUtils.hasText(detail)) {
            detail = ex.getMessage();
        }
        return detail;
    }

    private String wechatPayUserMessage(String detail) {
        if (!StringUtils.hasText(detail)) {
            return "请稍后重试或联系商家";
        }
        try {
            JsonNode node = objectMapper.readTree(detail);
            String code = text(node, "code");
            String message = text(node, "message");
            String mapped = mapWechatPayCode(code, message);
            if (StringUtils.hasText(mapped)) {
                return mapped;
            }
        } catch (Exception ignored) {
            // Keep the raw response in server logs; users only need an actionable Chinese prompt.
        }
        return "请稍后重试或联系商家";
    }

    private String mapWechatPayCode(String code, String message) {
        String normalized = trimToEmpty(code).toUpperCase();
        if ("PARAM_ERROR".equals(normalized)) {
            if (StringUtils.hasText(message) && message.contains("商品描述")) {
                return "活动标题过长，请联系商家处理后重试";
            }
            return "支付参数有误，请联系商家处理";
        }
        if ("NO_AUTH".equals(normalized)) {
            return "商户暂未开通或无权使用微信支付，请联系商家处理";
        }
        if ("SIGN_ERROR".equals(normalized)) {
            return "支付签名校验失败，请联系商家处理";
        }
        if ("ORDERPAID".equals(normalized)) {
            return "订单已支付，请勿重复支付";
        }
        if ("OUT_TRADE_NO_USED".equals(normalized)) {
            return "订单号已被使用，请重新下单";
        }
        if ("MCH_NOT_EXISTS".equals(normalized)) {
            return "商户号配置异常，请联系商家处理";
        }
        if ("INVALID_REQUEST".equals(normalized)) {
            return "支付请求无效，请稍后重试或联系商家";
        }
        return "";
    }

    private Map<String, String> successResponse() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("code", SUCCESS);
        result.put("message", "成功");
        return result;
    }

    private Map<String, String> failResponse(String message) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("code", FAIL);
        result.put("message", message);
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

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private void checkPayConfig() {
        if (!StringUtils.hasText(resolvePayAppId(isPartnerMode()))) {
            throw new IllegalArgumentException("WECHAT_APPID 未配置");
        }
        if (!StringUtils.hasText(properties.getPay().getMchid())) {
            throw new IllegalArgumentException("WECHAT_PAY_MCHID 未配置");
        }
        if (isPartnerMode() && !StringUtils.hasText(properties.getPay().getSubMchid())) {
            throw new IllegalArgumentException("WECHAT_PAY_SUB_MCHID 未配置");
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
        if (!StringUtils.hasText(properties.getPay().getNotifyUrl())) {
            throw new IllegalArgumentException("WECHAT_PAY_NOTIFY_URL 未配置");
        }
    }

    private boolean isPartnerMode() {
        return StringUtils.hasText(trimToEmpty(properties.getPay().getSubMchid()));
    }

    private String resolvePayAppId(boolean partnerMode) {
        if (partnerMode) {
            String subAppid = trimToEmpty(properties.getPay().getSubAppid());
            if (StringUtils.hasText(subAppid)) {
                return subAppid;
            }
        }
        return trimToEmpty(properties.getAppid());
    }

    private String resolveSpAppId() {
        String spAppid = trimToEmpty(properties.getPay().getSpAppid());
        if (StringUtils.hasText(spAppid)) {
            return spAppid;
        }
        String appid = trimToEmpty(properties.getAppid());
        if (StringUtils.hasText(appid)) {
            return appid;
        }
        throw new IllegalArgumentException("WECHAT_PAY_SP_APPID 未配置");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildOutRefundNo(String orderNo, boolean reclaimCommission) {
        String source = (reclaimCommission ? "RU" : "RA") + orderNo;
        return source.length() <= 64 ? source : source.substring(0, 64);
    }

    private boolean resolveRefundShouldReclaimCommission(String outRefundNo) {
        if (!StringUtils.hasText(outRefundNo)) {
            return true;
        }
        String value = outRefundNo.trim().toUpperCase();
        if (value.startsWith("RA")) {
            return false;
        }
        return true;
    }

    private boolean shouldSkipCommissionReclaim(IllegalArgumentException ex) {
        if (ex == null || ex.getMessage() == null) {
            return false;
        }
        return ex.getMessage().contains("已进入提现流程");
    }
}
