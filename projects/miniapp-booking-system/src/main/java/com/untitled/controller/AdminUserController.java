package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.UserCreateRequest;
import com.untitled.dto.UserUpdateRequest;
import com.untitled.model.User;
import com.untitled.service.UserService;
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
@RequestMapping("/api/admin/users")
@Validated
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<User>> list() {
        return ApiResponse.ok(userService.listUsers());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> get(@PathVariable long id) {
        return userService.getUser(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("用户不存在"));
    }

    @PostMapping
    public ApiResponse<User> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<User> update(@PathVariable long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("用户不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        if (userService.deleteUser(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("用户不存在");
    }
}
