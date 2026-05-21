package com.untitled.controller;

import com.untitled.dto.AdminWithdrawalUpdateRequest;
import com.untitled.dto.ApiResponse;
import com.untitled.dto.CommissionRecordResponse;
import com.untitled.dto.CommissionWithdrawalResponse;
import com.untitled.service.CommissionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminCommissionController {
    private final CommissionService commissionService;

    public AdminCommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping("/commissions") // 作用：声明后台佣金记录查询接口；方法：通过 GET 请求接收筛选条件
    public ApiResponse<List<CommissionRecordResponse>> listCommissions(@RequestParam(required = false) Long userId,
                                                                       // 作用：接收用户筛选条件；方法：从请求参数读取 userId
                                                                       @RequestParam(required = false) String orderNo,
                                                                       // 作用：接收订单号筛选条件；方法：从请求参数读取 orderNo
                                                                       @RequestParam(required = false) String status) {
        // 作用：接收佣金状态筛选条件；方法：从请求参数读取 status
        return ApiResponse.ok(commissionService.listAdminRecords(userId, orderNo, status));
        // 作用：返回佣金记录列表；方法：调用佣金服务查询后台佣金明细并封装响应
    }
    @GetMapping("/withdrawals") // 作用：声明后台提现列表接口；方法：通过 Spring MVC 暴露 GET /api/admin/withdrawals
    public ApiResponse<List<CommissionWithdrawalResponse>> listWithdrawals(@RequestParam(required = false) Long userId,
                                                                           // 作用：接收用户筛选条件；方法：从查询参数读取 userId
                                                                           @RequestParam(required = false) String status) {
        // 作用：接收状态筛选条件；方法：从查询参数读取提现状态
        return ApiResponse.ok(commissionService.listAdminWithdrawals(userId, status));
        // 作用：返回提现记录列表；方法：调用佣金服务查询并封装后台提现数据
    }

    @PutMapping("/withdrawals/{id}") // 作用：声明提现审核接口；方法：通过路径变量接收提现单 id
    public ApiResponse<CommissionWithdrawalResponse> updateWithdrawal(@PathVariable long id,
                                                                      // 作用：接收提现单编号；方法：从路径参数读取提现单 id
                                                                      @Valid @RequestBody AdminWithdrawalUpdateRequest request) {
        // 作用：接收审核参数；方法：从请求体读取目标状态和备注
        return ApiResponse.ok(commissionService.adminUpdateWithdrawal(id, request));
        // 作用：返回审核结果；方法：调用佣金服务更新提现状态并封装响应
    }
}
