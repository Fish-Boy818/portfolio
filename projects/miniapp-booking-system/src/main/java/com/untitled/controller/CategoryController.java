package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.CategoryResponse;
import com.untitled.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping // 作用：声明分类列表接口；方法：通过 Spring MVC 暴露 GET /api/categories
    /**
     * 作用：
     * 返回前台分类页需要的启用分类列表。
     * 方法：
     * 调用分类服务查询当前可用分类，再统一封装为接口响应返回前端。
     */
    public ApiResponse<List<CategoryResponse>> list() {
        // 作用：返回分类页数据；方法：调用分类服务获取启用分类后统一封装响应
        return ApiResponse.ok(categoryService.listActive());
        // 作用：返回启用分类列表；方法：调用 listActive 并使用 ApiResponse.ok 封装结果
    }
}
