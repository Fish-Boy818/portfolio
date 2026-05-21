package com.untitled.controller;

import com.untitled.dto.AdminCredentialResponse;
import com.untitled.dto.AdminCredentialUpdateRequest;
import com.untitled.dto.ApiResponse;
import com.untitled.service.AdminAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/admin/admin-account")
@Validated
public class AdminAccountController {
    private final AdminAuthService adminAuthService;

    public AdminAccountController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public ApiResponse<List<AdminCredentialResponse>> list() {
        return ApiResponse.ok(Collections.singletonList(adminAuthService.getCurrentCredential()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminCredentialResponse> get(@PathVariable long id) {
        return ApiResponse.ok(adminAuthService.getCredential(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCredentialResponse> update(@PathVariable long id,
                                                       @Valid @RequestBody AdminCredentialUpdateRequest request) {
        return ApiResponse.ok(adminAuthService.updateCredential(id, request));
    }
}
