package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.BountyTaskResponse;
import com.untitled.service.BountyTaskService;
import com.untitled.service.UserAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bounties")
public class BountyTaskController {
    private final BountyTaskService bountyTaskService;
    private final UserAuthService userAuthService;

    public BountyTaskController(BountyTaskService bountyTaskService,
                                UserAuthService userAuthService) {
        this.bountyTaskService = bountyTaskService;
        this.userAuthService = userAuthService;
    }

    @GetMapping
    public ApiResponse<List<BountyTaskResponse>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(bountyTaskService.listActive(userAuthService.resolveUserId(authorization)));
    }

    @GetMapping("/mine")
    public ApiResponse<List<BountyTaskResponse>> mine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(bountyTaskService.listMine(currentUserId));
    }

    @GetMapping("/{id}")
    public ApiResponse<BountyTaskResponse> get(@PathVariable long id,
                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        return bountyTaskService.get(id, false, userAuthService.resolveUserId(authorization))
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("悬赏任务不存在"));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<BountyTaskResponse> accept(@PathVariable long id,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return bountyTaskService.accept(id, currentUserId)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("悬赏任务不存在"));
    }
}
