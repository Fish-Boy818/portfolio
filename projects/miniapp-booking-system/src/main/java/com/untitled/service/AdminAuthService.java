package com.untitled.service;

import com.untitled.config.AdminProperties;
import com.untitled.dto.AdminCredentialResponse;
import com.untitled.dto.AdminCredentialUpdateRequest;
import com.untitled.mapper.AdminCredentialMapper;
import com.untitled.model.AdminCredential;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {
    private final AdminProperties properties;
    private final AdminCredentialMapper adminCredentialMapper;
    private final Map<String, AdminSession> tokens = new ConcurrentHashMap<>();
    private static final long TOKEN_TTL_MS = 24L * 60 * 60 * 1000;
    private static final long DEFAULT_ADMIN_ID = 1L;
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123456";

    public AdminAuthService(AdminProperties properties, AdminCredentialMapper adminCredentialMapper) {
        this.properties = properties;
        this.adminCredentialMapper = adminCredentialMapper;
    }
    public String login(String username, String password) { // 作用：处理后台账号密码登录；方法：校验管理员凭证后生成并保存 token
        AdminCredential credential = ensureCredential(); // 作用：获取后台账号配置；方法：读取数据库中的管理员凭证信息
        if (!credential.getUsername().equals(trimToEmpty(username)) || !credential.getPassword().equals(trimToEmpty(password))) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        String token = "admin-" + UUID.randomUUID().toString().replace("-", "");
        // 作用：生成后台访问令牌；方法：使用 UUID 构造唯一 token 字符串
        tokens.put(token, new AdminSession(credential.getUsername(), System.currentTimeMillis()));
        // 作用：保存后台登录态；方法：把 token 和会话对象写入内存容器
        return token; // 作用：返回后台令牌；方法：把生成的 token 作为登录结果返回
    }

    public boolean validate(String token) {
        return resolveSession(token) != null;
    }

    public String resolveUsername(String token) {
        AdminSession session = resolveSession(token);
        return session == null ? null : session.username;
    }

    public AdminCredentialResponse getCurrentCredential() {
        return toResponse(ensureCredential());
    }

    public AdminCredentialResponse getCredential(long id) {
        AdminCredential credential = ensureCredential();
        if (credential.getId() == null || credential.getId() != id) {
            throw new IllegalArgumentException("管理账号不存在");
        }
        return toResponse(credential);
    }

    @Transactional
    public AdminCredentialResponse updateCredential(long id, AdminCredentialUpdateRequest request) {
        AdminCredential credential = ensureCredential();
        if (credential.getId() == null || credential.getId() != id) {
            throw new IllegalArgumentException("管理账号不存在");
        }
        String username = trimToEmpty(request.getUsername());
        String currentPassword = trimToEmpty(request.getCurrentPassword());
        String password = trimToEmpty(request.getPassword());
        String confirmPassword = trimToEmpty(request.getConfirmPassword());
        if (!credential.getPassword().equals(currentPassword)) {
            throw new IllegalArgumentException("当前密码错误");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        credential.setUsername(username);
        credential.setPassword(password);
        adminCredentialMapper.update(credential);
        tokens.replaceAll((key, value) -> new AdminSession(username, value.issuedAt));
        return toResponse(adminCredentialMapper.findById(id));
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            tokens.remove(token);
        }
    }

    private AdminSession resolveSession(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        AdminSession session = tokens.get(token);
        if (session == null) {
            return null;
        }
        if (System.currentTimeMillis() - session.issuedAt > TOKEN_TTL_MS) {
            tokens.remove(token);
            return null;
        }
        return session;
    }

    private AdminCredential ensureCredential() {
        AdminCredential credential = adminCredentialMapper.findFirst();
        if (credential != null) {
            return credential;
        }
        credential = new AdminCredential();
        credential.setId(DEFAULT_ADMIN_ID);
        credential.setUsername(defaultUsername());
        credential.setPassword(defaultPassword());
        adminCredentialMapper.insert(credential);
        return adminCredentialMapper.findById(DEFAULT_ADMIN_ID);
    }

    private AdminCredentialResponse toResponse(AdminCredential credential) {
        AdminCredentialResponse response = new AdminCredentialResponse();
        response.setId(credential.getId());
        response.setUsername(credential.getUsername());
        response.setUpdatedAt(credential.getUpdatedAt());
        return response;
    }

    private String defaultUsername() {
        String username = trimToNull(properties.getUsername());
        return username != null ? username : DEFAULT_ADMIN_USERNAME;
    }

    private String defaultPassword() {
        String password = trimToNull(properties.getPassword());
        return password != null ? password : DEFAULT_ADMIN_PASSWORD;
    }

    private String trimToEmpty(String value) {
        String text = trimToNull(value);
        return text == null ? "" : text;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class AdminSession {
        private final String username;
        private final long issuedAt;

        private AdminSession(String username, long issuedAt) {
            this.username = username;
            this.issuedAt = issuedAt;
        }
    }
}
