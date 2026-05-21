package com.untitled.config;

import com.untitled.service.AdminAuthService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final AdminAuthService adminAuthService;

    public AdminAuthInterceptor(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Override // 作用：重写拦截器前置处理方法；方法：接入 Spring MVC 请求拦截流程
    /**
     * 作用：
     * 拦截后台接口并校验管理员登录状态。
     * 方法：
     * 先放行登录接口，再从请求头读取 X-Admin-Token，
     * 校验通过则继续执行请求，否则返回未登录 JSON 响应。
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException { // 作用：执行后台接口鉴权；方法：在控制器执行前检查管理员 token
        String path = request.getRequestURI(); // 作用：获取当前请求路径；方法：从 HttpServletRequest 中读取 URI
        if (path.startsWith("/api/admin/login")) {
            return true; // 作用：放行登录接口；方法：检测到登录路径后直接返回 true
        }
        String token = request.getHeader("X-Admin-Token"); // 作用：读取后台请求 token；方法：从 HTTP Header 中获取 X-Admin-Token
        if (adminAuthService.validate(token)) {
            return true; // 作用：放行已登录请求；方法：调用后台认证服务校验 token 后返回 true
        }
        response.setStatus(401); // 作用：设置未登录状态码；方法：向响应对象写入 HTTP 401 状态
        response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // 作用：设置响应编码；方法：指定返回内容为 UTF-8
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"); // 作用：声明响应类型；方法：把返回内容设置为 JSON
        response.getWriter().write("{\"code\":1,\"message\":\"请登录\",\"data\":null}"); // 作用：返回统一错误结构；方法：向响应体写入未登录 JSON 数据
        return false; // 作用：拦截后台请求；方法：返回 false 阻止后续控制器执行
    }
}
