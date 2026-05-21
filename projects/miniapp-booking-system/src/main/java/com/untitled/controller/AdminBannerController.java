package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.BannerCreateRequest;
import com.untitled.dto.BannerResponse;
import com.untitled.dto.BannerUpdateRequest;
import com.untitled.service.BannerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin/banners")
@Validated
public class AdminBannerController {
    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ApiResponse<List<BannerResponse>> list() {
        return ApiResponse.ok(bannerService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<BannerResponse> get(@PathVariable long id) {
        return bannerService.get(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("轮播图不存在"));
    }

    @PostMapping // 作用：声明轮播图新增接口；方法：通过 Spring MVC 暴露 POST /api/admin/banners
    /**
     * 作用：
     * 新增后台轮播图内容。
     * 方法：
     * 接收管理端提交的轮播标题、图片和排序等字段，
     * 调用轮播服务保存后返回新增结果。
     */
    public ApiResponse<BannerResponse> create(@Valid @RequestBody BannerCreateRequest request) { // 作用：处理轮播图新增请求；方法：接收轮播参数后调用服务保存数据
        return ApiResponse.ok(bannerService.create(request)); // 作用：返回新增轮播结果；方法：执行 bannerService.create 后统一封装响应
    }

    @PutMapping("/{id}")
    public ApiResponse<BannerResponse> update(@PathVariable long id, @Valid @RequestBody BannerUpdateRequest request) {
        return bannerService.update(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("轮播图不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        if (bannerService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("轮播图不存在");
    }
}
