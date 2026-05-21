package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.BountyTaskCreateRequest;
import com.untitled.dto.BountyTaskResponse;
import com.untitled.dto.BountyTaskUpdateRequest;
import com.untitled.service.BountyTaskService;
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
@RequestMapping("/api/admin/bounties")
@Validated
public class AdminBountyTaskController {
    private final BountyTaskService bountyTaskService;

    public AdminBountyTaskController(BountyTaskService bountyTaskService) {
        this.bountyTaskService = bountyTaskService;
    }

    @GetMapping
    public ApiResponse<List<BountyTaskResponse>> list() {
        return ApiResponse.ok(bountyTaskService.listAllForAdmin());
    }

    @GetMapping("/{id}")
    public ApiResponse<BountyTaskResponse> get(@PathVariable long id) {
        return bountyTaskService.get(id, true, null)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("悬赏任务不存在"));
    }

    @PostMapping
    public ApiResponse<BountyTaskResponse> create(@Valid @RequestBody BountyTaskCreateRequest request) {
        return ApiResponse.ok(bountyTaskService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BountyTaskResponse> update(@PathVariable long id, @Valid @RequestBody BountyTaskUpdateRequest request) {
        return bountyTaskService.update(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("悬赏任务不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        if (bountyTaskService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("悬赏任务不存在");
    }
}
