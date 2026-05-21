package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.StorageResult;
import com.untitled.service.StorageService;
import com.untitled.service.UploadCleanupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin")
public class AdminUploadController {
    private final StorageService storageService;
    private final UploadCleanupService uploadCleanupService;

    public AdminUploadController(StorageService storageService,
                                 UploadCleanupService uploadCleanupService) {
        this.storageService = storageService;
        this.uploadCleanupService = uploadCleanupService;
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "scene", required = false) String scene,
                                                   @RequestParam(value = "maxVideoSeconds", required = false) Integer maxVideoSeconds) throws IOException {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("请选择文件");
        }
        if ("banner".equalsIgnoreCase(scene) && isVideoFile(file) && !isMp4File(file.getOriginalFilename())) {
            return ApiResponse.fail("轮播视频仅支持 MP4 格式，请先转码后上传");
        }
        if (maxVideoSeconds != null && maxVideoSeconds < 0) {
            return ApiResponse.fail("maxVideoSeconds 不能小于 0");
        }
        StorageResult result = storageService.upload(file, maxVideoSeconds);
        Map<String, String> data = new HashMap<>();
        String url = result.getUrl();
        data.put("url", url);
        data.put("name", result.getName());
        data.put("mediaType", resolveMediaType(file, result.getName()));
        return ApiResponse.ok(data);
    }

    private String resolveMediaType(MultipartFile file, String fileName) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("video/")) {
            return "video";
        }
        if (StringUtils.hasText(fileName)) {
            String lower = fileName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v")
                    || lower.endsWith(".webm") || lower.endsWith(".avi") || lower.endsWith(".mkv")) {
                return "video";
            }
        }
        return "image";
    }

    private boolean isVideoFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("video/")) {
            return true;
        }
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) {
            return false;
        }
        String lower = original.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v")
                || lower.endsWith(".webm") || lower.endsWith(".avi") || lower.endsWith(".mkv");
    }

    private boolean isMp4File(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        return fileName.toLowerCase(Locale.ROOT).endsWith(".mp4");
    }

    @PostMapping("/uploads/delete")
    public ApiResponse<Void> delete(@RequestBody Map<String, String> payload) throws IOException {
        if (payload == null) {
            return ApiResponse.fail("缺少请求参数");
        }
        String url = payload.get("url");
        if (!StringUtils.hasText(url)) {
            return ApiResponse.fail("缺少文件地址");
        }
        storageService.delete(url);
        return ApiResponse.ok(null);
    }

    @PostMapping("/uploads/cleanup")
    public ApiResponse<?> cleanup(@RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) throws IOException {
        return ApiResponse.ok(uploadCleanupService.cleanup(dryRun));
    }

    @GetMapping("/uploads/missing")
    public ApiResponse<?> missing() throws IOException {
        return ApiResponse.ok(uploadCleanupService.detectMissing());
    }
}
