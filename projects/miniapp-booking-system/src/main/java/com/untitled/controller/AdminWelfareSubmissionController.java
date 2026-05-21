package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.WelfareSubmissionResponse;
import com.untitled.dto.WelfareSubmissionReviewRequest;
import com.untitled.service.WelfareSubmissionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/welfare-submissions")
@Validated
public class AdminWelfareSubmissionController {
    private final WelfareSubmissionService welfareSubmissionService;

    public AdminWelfareSubmissionController(WelfareSubmissionService welfareSubmissionService) {
        this.welfareSubmissionService = welfareSubmissionService;
    }

    @GetMapping
    public ApiResponse<List<WelfareSubmissionResponse>> list(@RequestParam(required = false) Long userId,
                                                             @RequestParam(required = false) String status) {
        return ApiResponse.ok(welfareSubmissionService.listForAdmin(userId, status));
    }

    @PutMapping("/{id}")
    public ApiResponse<WelfareSubmissionResponse> review(@PathVariable long id,
                                                         @RequestBody WelfareSubmissionReviewRequest request,
                                                         @RequestHeader(value = "X-Admin-User", required = false) String adminUser) {
        return ApiResponse.ok(welfareSubmissionService.review(id, request, adminUser));
    }
}
