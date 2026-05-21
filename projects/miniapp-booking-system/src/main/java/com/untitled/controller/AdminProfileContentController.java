package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.ProfileContentResponse;
import com.untitled.dto.ProfileContentUpdateRequest;
import com.untitled.service.ProfileContentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/profile-content")
@Validated
public class AdminProfileContentController {
    private final ProfileContentService profileContentService;

    public AdminProfileContentController(ProfileContentService profileContentService) {
        this.profileContentService = profileContentService;
    }

    @GetMapping
    public ApiResponse<List<ProfileContentResponse>> list() {
        return ApiResponse.ok(profileContentService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProfileContentResponse> get(@PathVariable long id) {
        return profileContentService.get(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("页面内容不存在"));
    }

    @PostMapping
    public ApiResponse<ProfileContentResponse> create(@Valid @RequestBody ProfileContentUpdateRequest request) {
        return ApiResponse.ok(profileContentService.create(request));
    }

    @PutMapping("/{id}") // 作用：声明页面内容更新接口；方法：通过路径变量接收页面内容 id
    /**
     * 作用：
     * 更新后台维护的个人中心页面内容。
     * 方法：
     * 根据页面内容 id 调用服务层执行字段更新，
     * 更新成功则返回最新配置，失败则返回不存在提示。
     */
    public ApiResponse<ProfileContentResponse> update(@PathVariable long id, @Valid @RequestBody ProfileContentUpdateRequest request) { // 作用：处理页面内容更新请求；方法：按 id 调用服务修改页面配置
        return profileContentService.update(id, request) // 作用：更新页面内容；方法：调用服务层按 id 更新数据库中的页面配置
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：把更新后的内容映射成统一成功响应
                .orElseGet(() -> ApiResponse.fail("页面内容不存在")); // 作用：返回失败结果；方法：页面内容不存在时构造失败响应
    }
}
