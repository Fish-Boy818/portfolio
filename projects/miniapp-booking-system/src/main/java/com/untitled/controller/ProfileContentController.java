package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.ProfileContentResponse;
import com.untitled.service.ProfileContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile-content")
public class ProfileContentController {
    private final ProfileContentService profileContentService;

    public ProfileContentController(ProfileContentService profileContentService) {
        this.profileContentService = profileContentService;
    }

    @GetMapping // 作用：声明个人中心页面内容接口；方法：通过 Spring MVC 暴露 GET /api/profile-content
    /**
     * 作用：
     * 返回个人中心页面展示所需的平台内容配置。
     * 方法：
     * 调用页面内容服务读取当前公告、关于我们和服务电话等配置，
     * 再统一封装为接口响应返回前端。
     */
    public ApiResponse<ProfileContentResponse> getCurrent() { // 作用：返回当前页面内容配置；方法：调用页面内容服务读取公告和关于我们信息
        return ApiResponse.ok(profileContentService.getCurrent()); // 作用：输出页面内容结果；方法：执行 getCurrent 后统一封装为成功响应
    }
}
