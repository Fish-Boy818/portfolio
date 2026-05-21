package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.UserCreateRequest;
import com.untitled.dto.UserUpdateRequest;
import com.untitled.dto.UserProfileResponse;
import com.untitled.dto.StorageResult;
import com.untitled.model.User;
import com.untitled.service.AdminAuthService;
import com.untitled.service.ProfileService;
import com.untitled.service.StorageService;
import com.untitled.service.UserAuthService;
import com.untitled.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final UserService userService;
    private final ProfileService profileService;
    private final UserAuthService userAuthService;
    private final AdminAuthService adminAuthService;
    private final StorageService storageService;

    public UserController(UserService userService,
                          ProfileService profileService,
                          UserAuthService userAuthService,
                          AdminAuthService adminAuthService,
                          StorageService storageService) {
        this.userService = userService;
        this.profileService = profileService;
        this.userAuthService = userAuthService;
        this.adminAuthService = adminAuthService;
        this.storageService = storageService;
    }

    @GetMapping
    public ApiResponse<List<User>> list(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return ApiResponse.ok(userService.listUsers());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> get(@PathVariable long id,
                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!canAccessUser(id, authorization, adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return userService.getUser(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("用户不存在"));
    }

    @GetMapping("/{id}/profile") // 作用：声明个人资料接口；方法：通过路径参数接收用户 id
    public ApiResponse<UserProfileResponse> profile(@PathVariable long id, // 作用：接收目标用户编号；方法：从路径参数读取用户 id
                                                    @RequestHeader(value = "Authorization", required = false) String authorization,
                                                    // 作用：接收前台登录凭证；方法：从请求头读取 Authorization
                                                    @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        // 作用：接收后台登录凭证；方法：从请求头读取 X-Admin-Token
        if (!canAccessUser(id, authorization, adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return userService.getUser(id) // 作用：查询用户实体；方法：调用用户服务按 id 读取用户数据
                .map(profileService::toProfile) // 作用：转换资料对象；方法：把 User 实体映射为 UserProfileResponse
                .map(ApiResponse::ok) // 作用：封装成功响应；方法：把资料对象包裹到 ApiResponse.ok 中
                .orElseGet(() -> ApiResponse.fail("用户不存在")); // 作用：返回失败信息；方法：用户不存在时构造失败响应
    }

    @PostMapping
    public ApiResponse<User> create(@Valid @RequestBody UserCreateRequest request,
                                    @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        return ApiResponse.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<User> update(@PathVariable long id,
                                    @Valid @RequestBody UserUpdateRequest request,
                                    @RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!canAccessUser(id, authorization, adminToken)) {
            return ApiResponse.fail("未授权");
        }
        boolean adminRequest = adminAuthService.validate(adminToken);
        if (!adminRequest) {
            User existing = userService.getUser(id).orElse(null);
            if (existing == null) {
                return ApiResponse.fail("用户不存在");
            }
            if (request.getPhone() != null && !Objects.equals(normalizeOptionalText(request.getPhone()), normalizeOptionalText(existing.getPhone()))) {
                return ApiResponse.fail("手机号与扫码资格绑定，暂不支持自行修改");
            }
        }
        return userService.updateUser(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("用户不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        if (!adminAuthService.validate(adminToken)) {
            return ApiResponse.fail("未授权");
        }
        if (userService.deleteUser(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("用户不存在");
    }

    @PostMapping("/{id}/avatar")
    public ApiResponse<User> uploadAvatar(@PathVariable long id,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) throws IOException {
        if (!canAccessUser(id, authorization, adminToken)) {
            return ApiResponse.fail("未授权");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("请选择头像文件");
        }
        if (!isImageFile(file)) {
            return ApiResponse.fail("仅支持图片格式头像");
        }
        StorageResult result = storageService.upload(file);
        return userService.updateAvatar(id, result.getUrl())
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("用户不存在"));
    }

    private boolean canAccessUser(long id, String authorization, String adminToken) {
        if (adminAuthService.validate(adminToken)) {
            return true;
        }
        Long currentUserId = userAuthService.resolveUserId(authorization);
        return currentUserId != null && currentUserId == id;
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

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
