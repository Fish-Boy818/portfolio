package com.untitled.controller;

import com.untitled.dto.ActivityCreateRequest;
import com.untitled.dto.ActivityResponse;
import com.untitled.dto.ActivityUpdateRequest;
import com.untitled.dto.ApiResponse;
import com.untitled.service.ActivityService;
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
import java.util.List;

@RestController
@RequestMapping("/api/admin/activities")
@Validated
public class AdminActivityController {
    private final ActivityService activityService;

    public AdminActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping // 作用：声明后台活动列表接口；方法：通过 GET 请求接收活动筛选条件
    public ApiResponse<List<ActivityResponse>> list(@RequestParam(required = false) Long clubId,
                                                    // 作用：接收俱乐部筛选条件；方法：从请求参数读取 clubId
                                                    @RequestParam(required = false) String category) {
        // 作用：接收分类筛选条件；方法：从请求参数读取 category
        return ApiResponse.ok(activityService.list(clubId, category, null, null));
        // 作用：返回活动列表；方法：调用活动服务按条件查询并封装响应
    }

    @GetMapping("/{id}") // 作用：声明后台活动详情接口；方法：通过路径参数接收活动 id
    public ApiResponse<ActivityResponse> get(@PathVariable long id) { // 作用：返回指定活动详情；方法：根据 id 调用活动服务查询活动信息
        return activityService.get(id, null) // 作用：查询活动详情；方法：调用 activityService.get(id, null) 按主键读取数据
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：查询成功后转换为统一成功响应
                .orElseGet(() -> ApiResponse.fail("活动不存在")); // 作用：返回失败提示；方法：活动不存在时构造失败响应
    }

    @PostMapping // 作用：声明后台新增活动接口；方法：通过 POST 请求接收活动创建数据
    public ApiResponse<ActivityResponse> create(@Valid @RequestBody ActivityCreateRequest request) {
        // 作用：新增活动信息；方法：接收并校验请求体后调用活动服务保存数据
        return ApiResponse.ok(activityService.create(request));
        // 作用：返回新增结果；方法：执行 activityService.create(request) 后统一封装响应
    }

    @PutMapping("/{id}") // 作用：声明后台修改活动接口；方法：通过路径参数接收活动 id 并更新数据
    public ApiResponse<ActivityResponse> update(@PathVariable long id, @Valid @RequestBody ActivityUpdateRequest request) {
        // 作用：修改指定活动信息；方法：根据 id 和请求体调用活动服务更新数据
        return activityService.update(id, request) // 作用：执行活动修改；方法：调用 activityService.update(id, request) 完成更新
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：更新成功后返回新的活动信息
                .orElseGet(() -> ApiResponse.fail("活动不存在")); // 作用：返回失败提示；方法：活动不存在时构造失败响应
    }

    @DeleteMapping("/{id}") // 作用：声明后台删除活动接口；方法：通过路径参数接收活动 id
    public ApiResponse<Void> delete(@PathVariable long id) { // 作用：删除指定活动；方法：根据 id 调用活动服务删除数据
        if (activityService.delete(id)) { // 作用：判断删除是否成功；方法：调用 activityService.delete(id) 获取删除结果
            return ApiResponse.ok(null); // 作用：返回删除成功结果；方法：使用统一响应体返回空数据
        }
        return ApiResponse.fail("活动不存在"); // 作用：返回删除失败提示；方法：删除目标不存在时构造失败响应
    }
}
