package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.SlotCreateRequest;
import com.untitled.dto.SlotResponse;
import com.untitled.dto.SlotUpdateRequest;
import com.untitled.service.AdminAuthService;
import com.untitled.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class SlotController {
    private final SlotService slotService;
    private final AdminAuthService adminAuthService;

    public SlotController(SlotService slotService, AdminAuthService adminAuthService) {
        this.slotService = slotService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/activities/{activityId}/slots")
    public ApiResponse<List<SlotResponse>> list(@PathVariable long activityId,
                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.ok(slotService.list(activityId, date));
    }

    @GetMapping("/slots")
    public ApiResponse<List<SlotResponse>> listAll(@RequestParam(required = false) Long activityId) {
        return ApiResponse.ok(slotService.listAll(activityId));
    }

    @PostMapping("/activities/{activityId}/slots")
    public ApiResponse<SlotResponse> create(@PathVariable long activityId,
                                            @Valid @RequestBody SlotCreateRequest request,
                                            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        request.setActivityId(activityId);
        return ApiResponse.ok(slotService.create(request));
    }

    @PutMapping("/slots/{id}")
    public ApiResponse<SlotResponse> update(@PathVariable long id,
                                            @Valid @RequestBody SlotUpdateRequest request,
                                            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return slotService.update(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("时段不存在"));
    }

    @DeleteMapping("/slots/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        if (slotService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("时段不存在");
    }
}
