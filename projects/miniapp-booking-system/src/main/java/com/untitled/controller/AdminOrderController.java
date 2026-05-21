package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.AdminOrderRefundRequest;
import com.untitled.dto.OrderCreateRequest;
import com.untitled.dto.OrderResponse;
import com.untitled.dto.OrderUpdateRequest;
import com.untitled.service.OrderService;
import com.untitled.service.WechatPayService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@Validated
public class AdminOrderController {
    private final OrderService orderService;
    private final WechatPayService wechatPayService;

    public AdminOrderController(OrderService orderService, WechatPayService wechatPayService) {
        this.orderService = orderService;
        this.wechatPayService = wechatPayService;
    }

    @GetMapping // 作用：声明后台订单列表接口；方法：通过 Spring MVC 暴露 GET /api/admin/orders
    public ApiResponse<List<OrderResponse>> list(@RequestParam(required = false) Long userId, // 作用：接收用户筛选条件；方法：从查询参数读取 userId
                                                 @RequestParam(required = false) String status) { // 作用：接收状态筛选条件；方法：从查询参数读取订单状态
        return ApiResponse.ok(orderService.list(userId, status)); // 作用：返回后台订单列表；方法：调用订单服务按条件查询并封装响应
    }

    @GetMapping("/export") // 作用：声明后台订单导出接口；方法：通过 GET 请求导出筛选后的订单数据
    public void export(@RequestParam(required = false) Long userId, // 作用：接收用户筛选条件；方法：从请求参数读取 userId
                       @RequestParam(required = false) String status, // 作用：接收状态筛选条件；方法：从请求参数读取订单状态
                       HttpServletResponse response) throws IOException { // 作用：输出导出文件；方法：通过响应对象向浏览器写出 CSV 内容
        List<OrderResponse> orders = orderService.list(userId, status); // 作用：查询导出订单数据；方法：按筛选条件调用订单服务获取订单列表
        String filename = "orders_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        // 作用：生成导出文件名；方法：使用当前时间拼接唯一 CSV 文件名
        response.setContentType("text/csv;charset=UTF-8"); // 作用：设置响应类型；方法：声明返回内容为 UTF-8 编码的 CSV
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, "UTF-8")); // 作用：设置下载响应头；方法：通过 attachment 触发浏览器下载文件
        StringBuilder builder = new StringBuilder(); // 作用：创建字符串构造器；方法：用于拼接完整的 CSV 文本内容
        builder.append('\uFEFF'); // 作用：写入 BOM 头；方法：保证 Excel 打开 CSV 时中文不乱码
        builder.append("订单号,状态,活动,俱乐部,用户手机号,数量,实付金额,单价,原价,价格级别,扫码用户,商家结算,平台运营费,用户佣金,出发日期,出发时间,下单时间,核销码\n"); // 作用：写入表头；方法：按固定字段顺序拼接 CSV 第一行
        for (OrderResponse order : orders) { // 作用：遍历订单数据；方法：逐条把订单字段写入 CSV
            builder.append(csv(order.getOrderNo())).append(','); // 作用：写入订单号；方法：格式化订单号后追加分隔符
            builder.append(csv(order.getStatusText())).append(','); // 作用：写入订单状态；方法：格式化状态文本后追加分隔符
            builder.append(csv(order.getTitle())).append(','); // 作用：写入活动名称；方法：格式化活动标题后追加分隔符
            builder.append(csv(order.getClubName())).append(','); // 作用：写入俱乐部名称；方法：格式化门店名称后追加分隔符
            builder.append(csv(order.getUserPhone())).append(','); // 作用：写入用户手机号；方法：格式化手机号后追加分隔符
            builder.append(csv(order.getQuantity())).append(','); // 作用：写入购买数量；方法：格式化数量后追加分隔符
            builder.append(csv(order.getPayAmount())).append(','); // 作用：写入实付金额；方法：格式化支付金额后追加分隔符
            builder.append(csv(order.getUnitPrice())).append(','); // 作用：写入单价；方法：格式化单价后追加分隔符
            builder.append(csv(order.getOriginalPrice())).append(','); // 作用：写入原价；方法：格式化原价后追加分隔符
            builder.append(csv(order.getPriceLevel())).append(','); // 作用：写入价格级别；方法：格式化价格级别后追加分隔符
            builder.append(csv(order.getScanUserAtOrderTime())).append(','); // 作用：写入扫码用户标识；方法：格式化扫码状态后追加分隔符
            builder.append(csv(order.getMerchantSettlementAmount())).append(','); // 作用：写入商家结算金额；方法：格式化结算金额后追加分隔符
            builder.append(csv(order.getPlatformOperationFee())).append(','); // 作用：写入平台运营费；方法：格式化平台费用后追加分隔符
            builder.append(csv(order.getUserCommissionAmount())).append(','); // 作用：写入用户佣金；方法：格式化佣金金额后追加分隔符
            builder.append(csv(order.getSlotDate())).append(','); // 作用：写入出发日期；方法：格式化预约日期后追加分隔符
            builder.append(csv(order.getSlotTime())).append(','); // 作用：写入出发时间；方法：格式化预约时间后追加分隔符
            builder.append(csv(order.getCreatedAt())).append(','); // 作用：写入下单时间；方法：格式化创建时间后追加分隔符
            builder.append(csv(order.getVerifyCode())).append('\n'); // 作用：写入核销码并换行；方法：格式化核销码后结束当前记录
        }
        response.getWriter().write(builder.toString()); // 作用：写出 CSV 内容；方法：将拼接后的字符串写入响应输出流
        response.getWriter().flush(); // 作用：刷新输出流；方法：确保导出内容立即发送到客户端
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        text = text.replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> get(@PathVariable long id) {
        return orderService.get(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("订单不存在"));
    }

    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<OrderResponse> update(@PathVariable long id, @Valid @RequestBody OrderUpdateRequest request) {
        if ("refund".equals(request.getStatus())) {
            return ApiResponse.ok(wechatPayService.createRefund(id, true));
        }
        return orderService.updateStatus(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("订单不存在"));
    }

    @PostMapping("/{id}/refund") // 作用：声明后台订单退款接口；方法：通过路径变量接收订单 id
    /**
     * 作用：
     * 处理后台对指定订单的退款请求。
     * 方法：
     * 先读取是否需要回收返利的参数，
     * 再调用微信退款服务完成订单退款并返回结果。
     */
    public ApiResponse<OrderResponse> refund(@PathVariable long id, // 作用：接收订单编号；方法：从路径参数读取订单 id
                                             @RequestBody(required = false) AdminOrderRefundRequest request) { // 作用：接收退款参数；方法：从请求体读取是否回收佣金配置
        boolean reclaimCommission = request == null || request.getReclaimCommission() == null || request.getReclaimCommission(); // 作用：判断是否回收返利；方法：从请求参数中读取 reclaimCommission，空值时默认回收
        return ApiResponse.ok(wechatPayService.createRefund(id, reclaimCommission)); // 作用：返回退款结果；方法：调用微信退款逻辑并封装更新后的订单数据
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        if (orderService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("订单不存在");
    }
}
