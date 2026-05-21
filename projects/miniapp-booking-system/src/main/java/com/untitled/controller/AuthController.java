package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.AuthResponse;
import com.untitled.dto.AuthWechatPhoneRequest;
import com.untitled.dto.AuthWechatRequest;
import com.untitled.dto.UserProfileResponse;
import com.untitled.dto.WechatSessionResponse;
import com.untitled.model.User;
import com.untitled.service.ProfileService;
import com.untitled.service.UserService;
import com.untitled.service.UserAuthService;
import com.untitled.service.WechatAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final UserService userService;
    private final ProfileService profileService;
    private final WechatAuthService wechatAuthService;
    private final UserAuthService userAuthService;

    public AuthController(UserService userService,
                          ProfileService profileService,
                          WechatAuthService wechatAuthService,
                          UserAuthService userAuthService) {
        this.userService = userService;
        this.profileService = profileService;
        this.wechatAuthService = wechatAuthService;
        this.userAuthService = userAuthService;
    }

    @PostMapping("/wechat")
    public ApiResponse<AuthResponse> wechatLogin(@Valid @RequestBody AuthWechatRequest request) {
        WechatSessionResponse session = wechatAuthService.getSession(request.getCode());
        String openId = session.getOpenid();
        User user = userService.getOrCreateByOpenId(openId, request.getNickname(), request.getAvatarUrl());
        UserProfileResponse profile = profileService.toProfile(user);
        AuthResponse response = new AuthResponse();
        response.setToken(userAuthService.issueToken(user.getId()));
        response.setUser(profile);
        return ApiResponse.ok(response);
    }

    @PostMapping("/wechat/phone") // 作用：声明微信手机号登录接口；方法：通过 Spring MVC 将该方法映射到 /api/auth/wechat/phone
    /**
     * 作用：
     * 实现微信小程序手机号授权登录，并返回平台登录态。
     * 方法：
     * 先通过微信登录凭证换取 openId，再通过手机号授权码换取手机号，
     * 随后按 openId 查询或创建用户，最后签发 token 并返回用户资料。
     */
    public ApiResponse<AuthResponse> wechatPhoneLogin(@Valid @RequestBody AuthWechatPhoneRequest request) {
        // 作用：处理微信手机号授权登录请求；方法：接收前端 code 和 phoneCode 后执行完整登录流程
        WechatSessionResponse session = wechatAuthService.getSession(request.getCode());
        // 作用：获取微信登录会话；方法：调用微信接口根据 code 换取 session 和 openId
        String openId = session.getOpenid(); // 作用：提取用户唯一标识；方法：从微信会话对象中读取 openId 字段
        String phone = wechatAuthService.getPhoneNumberByCode(request.getPhoneCode());
        // 作用：获取用户手机号；方法：调用微信手机号接口根据 phoneCode 解密手机号
        User user = userService.getOrCreateByOpenId(openId, request.getNickname(), request.getAvatarUrl(), phone);
        // 作用：完成用户落库；方法：按 openId 查询用户，不存在则创建，存在则更新资料
        UserProfileResponse profile = profileService.toProfile(user);
        // 作用：组装前端用户资料；方法：将用户实体转换为用户资料响应对象
        AuthResponse response = new AuthResponse();
        // 作用：创建登录响应对象；方法：实例化统一的认证返回结构
        response.setToken(userAuthService.issueToken(user.getId())); // 作用：生成登录态；方法：根据用户 id 签发平台 token
        response.setUser(profile); // 作用：写入用户资料；方法：把转换后的 profile 填充到响应对象中
        return ApiResponse.ok(response); // 作用：返回登录结果；方法：使用统一响应体封装成功数据
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        userAuthService.revoke(authorization);
        return ApiResponse.ok(null);
    }
}
