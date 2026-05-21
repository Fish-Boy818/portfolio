package com.untitled.controller;

import com.untitled.dto.AdminLoginRequest;
import com.untitled.dto.AdminLoginResponse;
import com.untitled.dto.ApiResponse;
import com.untitled.service.AdminAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login") // 作用：声明后台登录接口；方法：通过 Spring MVC 暴露 POST /api/admin/login
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        // 作用：处理后台登录请求；方法：接收管理员账号密码并调用认证服务
        String token = adminAuthService.login(request.getUsername(), request.getPassword());
        // 作用：生成后台登录 token；方法：校验用户名密码后由认证服务签发 token
        AdminLoginResponse response = new AdminLoginResponse();
        // 作用：创建后台登录返回对象；方法：实例化 AdminLoginResponse 作为响应体
        response.setToken(token);
        response.setUsername(adminAuthService.resolveUsername(token));
        // 作用：写入管理员用户名；方法：根据 token 查询当前登录管理员名称
        return ApiResponse.ok(response); // 作用：返回后台登录结果；方法：使用统一响应对象封装管理员信息
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) AdminLoginResponse request) {
        if (request != null) {
            adminAuthService.logout(request.getToken());
        }
        return ApiResponse.ok(null);
    }
}
