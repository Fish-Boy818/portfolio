package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.StorageResult;
import com.untitled.dto.WelfareSubmissionCreateRequest;
import com.untitled.dto.WelfareSubmissionResponse;
import com.untitled.service.StorageService;
import com.untitled.service.UserAuthService;
import com.untitled.service.WelfareSubmissionService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/welfare")
@Validated
public class WelfareController {
    private final WelfareSubmissionService welfareSubmissionService;
    private final UserAuthService userAuthService;
    private final StorageService storageService;

    public WelfareController(WelfareSubmissionService welfareSubmissionService,
                             UserAuthService userAuthService,
                             StorageService storageService) {
        this.welfareSubmissionService = welfareSubmissionService;
        this.userAuthService = userAuthService;
        this.storageService = storageService;
    }

    @GetMapping("/submissions")
    public ApiResponse<List<WelfareSubmissionResponse>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(welfareSubmissionService.listByUser(currentUserId));
    }

    @PostMapping("/submissions")
    public ApiResponse<WelfareSubmissionResponse> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                         @Valid @RequestBody WelfareSubmissionCreateRequest request) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(welfareSubmissionService.create(currentUserId, request));
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestParam("file") MultipartFile file) throws IOException {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("请选择图片");
        }
        if (!isImageFile(file)) {
            return ApiResponse.fail("仅支持上传图片");
        }
        StorageResult result = storageService.upload(file);
        Map<String, String> data = new HashMap<String, String>();
        data.put("url", result.getUrl());
        data.put("name", result.getName());
        return ApiResponse.ok(data);
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) {
            return false;
        }
        String lower = original.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.endsWith(".bmp");
    }
}
