package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.ClubCreateRequest;
import com.untitled.dto.ClubResponse;
import com.untitled.dto.ClubUpdateRequest;
import com.untitled.service.ClubService;
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
@RequestMapping("/api/admin/clubs")
@Validated
public class AdminClubController {
    private final ClubService clubService;

    public AdminClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping // 作用：声明后台俱乐部列表接口；方法：通过 Spring MVC 暴露 GET /api/admin/clubs
    public ApiResponse<List<ClubResponse>> list() { // 作用：返回后台俱乐部列表；方法：调用俱乐部服务查询全部门店并封装响应
        return ApiResponse.ok(clubService.list()); // 作用：输出俱乐部列表结果；方法：执行 clubService.list() 后统一返回
    }

    @GetMapping("/{id}") // 作用：声明后台俱乐部详情接口；方法：通过路径参数接收俱乐部 id
    public ApiResponse<ClubResponse> get(@PathVariable long id) { // 作用：返回指定俱乐部详情；方法：根据 id 调用俱乐部服务查询门店信息
        return clubService.get(id) // 作用：查询俱乐部详情；方法：调用 clubService.get(id) 按主键读取数据
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：查询成功后转换为统一成功响应
                .orElseGet(() -> ApiResponse.fail("俱乐部不存在")); // 作用：返回失败提示；方法：俱乐部不存在时构造失败响应
    }

    @PostMapping // 作用：声明后台新增俱乐部接口；方法：通过 POST 请求接收俱乐部创建数据
    public ApiResponse<ClubResponse> create(@Valid @RequestBody ClubCreateRequest request) {
        // 作用：新增俱乐部信息；方法：接收并校验请求体后调用服务层保存
        return ApiResponse.ok(clubService.create(request)); // 作用：返回新增结果；方法：执行 clubService.create(request) 后统一封装响应
    }

    @PutMapping("/{id}") // 作用：声明后台修改俱乐部接口；方法：通过路径参数接收俱乐部 id 并更新数据
    public ApiResponse<ClubResponse> update(@PathVariable long id, @Valid @RequestBody ClubUpdateRequest request) {
        // 作用：修改指定俱乐部信息；方法：根据 id 和请求体调用服务层更新门店数据
        return clubService.update(id, request) // 作用：执行俱乐部修改；方法：调用 clubService.update(id, request) 完成更新
                .map(ApiResponse::ok) // 作用：封装成功结果；方法：更新成功后返回新的俱乐部信息
                .orElseGet(() -> ApiResponse.fail("俱乐部不存在")); // 作用：返回失败提示；方法：俱乐部不存在时构造失败响应
    }

    @DeleteMapping("/{id}") // 作用：声明后台删除俱乐部接口；方法：通过路径参数接收俱乐部 id
    public ApiResponse<Void> delete(@PathVariable long id) { // 作用：删除指定俱乐部；方法：根据 id 调用服务层删除门店数据
        if (clubService.delete(id)) { // 作用：判断删除是否成功；方法：调用 clubService.delete(id) 获取布尔结果
            return ApiResponse.ok(null); // 作用：返回删除成功结果；方法：使用统一响应体返回空数据
        }
        return ApiResponse.fail("俱乐部不存在"); // 作用：返回删除失败提示；方法：删除目标不存在时构造失败响应
    }
}
