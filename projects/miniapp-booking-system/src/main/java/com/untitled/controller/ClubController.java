package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.ClubCreateRequest;
import com.untitled.dto.ClubResponse;
import com.untitled.dto.ClubUpdateRequest;
import com.untitled.service.AdminAuthService;
import com.untitled.service.ClubService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@Validated
public class ClubController {
    private final ClubService clubService;
    private final AdminAuthService adminAuthService;

    public ClubController(ClubService clubService, AdminAuthService adminAuthService) {
        this.clubService = clubService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping // 作用：声明俱乐部列表接口；方法：通过 Spring MVC 暴露 GET /api/clubs
    public ApiResponse<List<ClubResponse>> list() {
        // 作用：返回俱乐部列表；方法：调用俱乐部服务读取全部门店并封装响应
        return ApiResponse.ok(clubService.list());
        // 作用：输出俱乐部列表结果；方法：执行 service.list 后使用 ApiResponse.ok 返回
    }

    @GetMapping("/{id}") // 作用：声明俱乐部详情接口；方法：通过路径变量接收俱乐部 id
    public ApiResponse<ClubResponse> get(@PathVariable long id) {
        // 作用：返回指定俱乐部详情；方法：按 id 查询俱乐部后判断是否存在
        return clubService.get(id) // 作用：查询俱乐部详情；方法：调用俱乐部服务按主键读取数据
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：把查询到的俱乐部对象映射成成功响应
                .orElseGet(() -> ApiResponse.fail("俱乐部不存在"));
        // 作用：返回失败提示；方法：未找到俱乐部时构造失败响应
    }

    @PostMapping
    public ApiResponse<ClubResponse> create(@Valid @RequestBody ClubCreateRequest request,
                                            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return ApiResponse.ok(clubService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ClubResponse> update(@PathVariable long id,
                                            @Valid @RequestBody ClubUpdateRequest request,
                                            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return clubService.update(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("俱乐部不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        if (clubService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("俱乐部不存在");
    }
}
