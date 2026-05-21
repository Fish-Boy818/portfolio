package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.OrderResponse;
import com.untitled.dto.WechatPayParamsResponse;
import com.untitled.service.OrderService;
import com.untitled.service.UserAuthService;
import com.untitled.service.WechatPayService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/pay")
public class PayController {
    private final WechatPayService wechatPayService;
    private final OrderService orderService;
    private final UserAuthService userAuthService;

    public PayController(WechatPayService wechatPayService, OrderService orderService, UserAuthService userAuthService) {
        this.wechatPayService = wechatPayService;
        this.orderService = orderService;
        this.userAuthService = userAuthService;
    }

    @PostMapping("/orders/{id}/jsapi")
    public ApiResponse<WechatPayParamsResponse> jsapiPay(@PathVariable long id,
                                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        java.util.Optional<OrderResponse> target = orderService.get(id);
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("无权支付该订单");
        }
        return ApiResponse.ok(wechatPayService.createJsapiPay(id));
    }

    @PostMapping("/notify")
    public Map<String, String> notifyPay(@RequestBody String payload, HttpServletRequest request) {
        return wechatPayService.handleNotify(
                payload,
                request.getHeader("Wechatpay-Timestamp"),
                request.getHeader("Wechatpay-Nonce"),
                request.getHeader("Wechatpay-Signature"),
                request.getHeader("Wechatpay-Serial")
        );
    }

    @PostMapping("/refund/notify")
    public Map<String, String> notifyRefund(@RequestBody String payload, HttpServletRequest request) {
        return wechatPayService.handleNotify(
                payload,
                request.getHeader("Wechatpay-Timestamp"),
                request.getHeader("Wechatpay-Nonce"),
                request.getHeader("Wechatpay-Signature"),
                request.getHeader("Wechatpay-Serial")
        );
    }
}
