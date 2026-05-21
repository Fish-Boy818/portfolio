package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.CategoryCreateRequest;
import com.untitled.dto.CategoryResponse;
import com.untitled.dto.CategoryUpdateRequest;
import com.untitled.service.CategoryService;
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
@RequestMapping("/api/admin/categories")
@Validated
public class AdminCategoryController {
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping // 作用：声明后台分类列表接口；方法：通过 GET 请求返回全部分类
    public ApiResponse<List<CategoryResponse>> list() { // 作用：返回后台分类列表；方法：调用分类服务查询全部分类并封装响应
        return ApiResponse.ok(categoryService.listAll()); // 作用：输出分类列表结果；方法：执行 categoryService.listAll() 后统一返回
    }

    @GetMapping("/{id}") // 作用：声明后台分类详情接口；方法：通过路径参数接收分类 id
    public ApiResponse<CategoryResponse> get(@PathVariable long id) { // 作用：返回指定分类详情；方法：根据 id 调用分类服务查询分类信息
        return categoryService.get(id) // 作用：查询分类详情；方法：调用 categoryService.get(id) 按主键读取数据
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：查询成功后转换为统一成功响应
                .orElseGet(() -> ApiResponse.fail("分类不存在")); // 作用：返回失败提示；方法：分类不存在时构造失败响应
    }

    @PostMapping // 作用：声明后台新增分类接口；方法：通过 POST 请求接收分类创建数据
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        // 作用：新增分类信息；方法：接收并校验请求体后调用分类服务保存数据
        return ApiResponse.ok(categoryService.create(request));
        // 作用：返回新增结果；方法：执行 categoryService.create(request) 后统一封装响应
    }

    @PutMapping("/{id}") // 作用：声明后台修改分类接口；方法：通过路径参数接收分类 id 并更新数据
    public ApiResponse<CategoryResponse> update(@PathVariable long id, @Valid @RequestBody CategoryUpdateRequest request) {
        // 作用：修改指定分类信息；方法：根据 id 和请求体调用分类服务更新数据
        return categoryService.update(id, request) // 作用：执行分类修改；方法：调用 categoryService.update(id, request) 完成更新
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：更新成功后返回新的分类信息
                .orElseGet(() -> ApiResponse.fail("分类不存在")); // 作用：返回失败提示；方法：分类不存在时构造失败响应
    }

    @DeleteMapping("/{id}") // 作用：声明后台删除分类接口；方法：通过路径参数接收分类 id
    public ApiResponse<Void> delete(@PathVariable long id) { // 作用：删除指定分类；方法：根据 id 调用分类服务删除数据
        if (categoryService.delete(id)) { // 作用：判断删除是否成功；方法：调用 categoryService.delete(id) 获取删除结果
            return ApiResponse.ok(null); // 作用：返回删除成功结果；方法：使用统一响应体返回空数据
        }
        return ApiResponse.fail("分类不存在"); // 作用：返回删除失败提示；方法：删除目标不存在时构造失败响应
    }
}
