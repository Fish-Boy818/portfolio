package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.OrderCreateRequest;
import com.untitled.dto.OrderResponse;
import com.untitled.dto.OrderUpdateRequest;
import com.untitled.service.OrderService;
import com.untitled.service.UserAuthService;
import com.untitled.service.WechatPayService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    private final OrderService orderService;
    private final WechatPayService wechatPayService;
    private final UserAuthService userAuthService;

    public OrderController(OrderService orderService, WechatPayService wechatPayService, UserAuthService userAuthService) {
        this.orderService = orderService;
        this.wechatPayService = wechatPayService;
        this.userAuthService = userAuthService;
    }

    @GetMapping // 作用：声明订单列表接口；方法：通过 Spring MVC 暴露 GET /api/orders
    public ApiResponse<List<OrderResponse>> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 // 作用：接收登录凭证；方法：从请求头读取 Authorization
                                                 @RequestParam(required = false) Long userId,
                                                 // 作用：接收用户筛选条件；方法：从查询参数读取 userId
                                                 @RequestParam(required = false) String status) {
        // 作用：接收状态筛选条件；方法：从查询参数读取订单状态
        Long currentUserId = userAuthService.resolveUserId(authorization);
        // 作用：识别当前登录用户；方法：通过 token 解析用户 id
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        if (userId != null && !currentUserId.equals(userId)) {
            return ApiResponse.fail("无权查看他人订单");
        }
        return ApiResponse.ok(orderService.list(currentUserId, status));
        // 作用：返回订单列表；方法：按当前用户和状态条件查询订单并封装响应
    }

    @GetMapping("/{id}") // 作用：声明订单详情接口；方法：通过路径变量接收订单 id
    /**
     * 作用：
     * 返回当前登录用户的指定订单详情。
     * 方法：
     * 先校验登录状态并查询订单，再判断订单归属是否属于当前用户，
     * 最后返回订单详情数据。
     */
    public ApiResponse<OrderResponse> get(@PathVariable long id, // 作用：接收订单编号；方法：从路径参数读取订单 id
                                          @RequestHeader(value = "Authorization", required = false) String authorization) { // 作用：接收登录凭证；方法：从请求头读取 Authorization
        Long currentUserId = userAuthService.resolveUserId(authorization); // 作用：识别当前登录用户；方法：通过 token 解析用户 id
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Optional<OrderResponse> target = orderService.get(id); // 作用：查询订单详情；方法：调用订单服务按 id 读取订单
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("订单不存在");
        }
        return ApiResponse.ok(target.get()); // 作用：返回订单详情；方法：将订单对象封装为统一成功响应
    }

    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        if (request.getUserId() != null && !currentUserId.equals(request.getUserId())) {
            return ApiResponse.fail("无权为他人下单");
        }
        request.setUserId(currentUserId);
        return ApiResponse.ok(orderService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<OrderResponse> update(@PathVariable long id,
                                             @Valid @RequestBody OrderUpdateRequest request,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Optional<OrderResponse> target = orderService.get(id);
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("无权操作该订单");
        }
        String status = request.getStatus();
        if ("cancelled".equals(status)) {
            Optional<OrderResponse> response = orderService.cancel(id);
            return response.map(ApiResponse::ok).orElseGet(() -> ApiResponse.fail("订单不存在"));
        }
        if ("unpaid".equals(status)) {
            return ApiResponse.ok(target.get());
        }
        return ApiResponse.fail("不允许直接修改该订单状态");
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderResponse> pay(@PathVariable long id,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Optional<OrderResponse> target = orderService.get(id);
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("无权支付该订单");
        }
        return ApiResponse.ok(wechatPayService.confirmPaid(id));
    }

    @PostMapping("/{id}/verify")
    public ApiResponse<OrderResponse> verify(@PathVariable long id,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Optional<OrderResponse> target = orderService.get(id);
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("无权核销该订单");
        }
        Optional<OrderResponse> response = orderService.verify(id);
        return response.map(ApiResponse::ok).orElseGet(() -> ApiResponse.fail("订单不存在"));
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<OrderResponse> refund(@PathVariable long id,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Optional<OrderResponse> target = orderService.get(id);
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("无权退款该订单");
        }
        return ApiResponse.ok(wechatPayService.createRefund(id, true));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable long id,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Optional<OrderResponse> target = orderService.get(id);
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("无权取消该订单");
        }
        Optional<OrderResponse> response = orderService.cancel(id);
        return response.map(ApiResponse::ok).orElseGet(() -> ApiResponse.fail("订单不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Optional<OrderResponse> target = orderService.get(id);
        if (!target.isPresent()) {
            return ApiResponse.fail("订单不存在");
        }
        if (!currentUserId.equals(target.get().getUserId())) {
            return ApiResponse.fail("无权删除该订单");
        }
        if (orderService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("订单不存在");
    }
}
