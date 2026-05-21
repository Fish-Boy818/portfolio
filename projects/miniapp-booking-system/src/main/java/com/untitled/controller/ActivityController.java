package com.untitled.controller;

import com.untitled.dto.ActivityCreateRequest;
import com.untitled.dto.ActivityResponse;
import com.untitled.dto.ActivityUpdateRequest;
import com.untitled.dto.ApiResponse;
import com.untitled.service.AdminAuthService;
import com.untitled.service.ActivityService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@Validated
public class ActivityController {
    private final ActivityService activityService;
    private final AdminAuthService adminAuthService;

    public ActivityController(ActivityService activityService, AdminAuthService adminAuthService) {
        this.activityService = activityService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public ApiResponse<List<ActivityResponse>> list(@RequestParam(required = false) Long clubId,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long userId) {
        return ApiResponse.ok(activityService.list(clubId, category, keyword, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityResponse> get(@PathVariable long id,
                                             @RequestParam(required = false) Long userId) {
        return activityService.get(id, userId)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("活动不存在"));
    }

    @PostMapping
    public ApiResponse<ActivityResponse> create(@Valid @RequestBody ActivityCreateRequest request,
                                                @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return ApiResponse.ok(activityService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ActivityResponse> update(@PathVariable long id,
                                                @Valid @RequestBody ActivityUpdateRequest request,
                                                @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return activityService.update(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("活动不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        if (activityService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("活动不存在");
    }
}
