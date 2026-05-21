package com.untitled.service;

import com.untitled.dto.UserCreateRequest;
import com.untitled.dto.UserUpdateRequest;
import com.untitled.mapper.UserMapper;
import com.untitled.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<User> listUsers() {
        return userMapper.findAll();
    }

    public Optional<User> getUser(long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }

    public User createUser(UserCreateRequest request) {
        User user = new User();
        String openId = request.getOpenId();
        if (openId == null || openId.trim().isEmpty()) {
            openId = "manual-" + System.currentTimeMillis();
        }
        user.setOpenId(openId);
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setPhone(request.getPhone());
        user.setDepth(normalizeDepth(request.getDepth()));
        user.setScanUser(0);
        userMapper.insert(user);
        return userMapper.findById(user.getId());
    }

    public Optional<User> updateUser(long id, UserUpdateRequest request) {
        User existing = userMapper.findById(id);
        if (existing == null) {
            return Optional.empty();
        }
        existing.setNickname(request.getNickname());
        existing.setAvatarUrl(request.getAvatarUrl());
        existing.setPhone(request.getPhone());
        Integer normalizedScanUser = normalizeScanUser(request.getScanUser());
        if (normalizedScanUser != null) {
            existing.setScanUser(normalizedScanUser);
            if (normalizedScanUser == 1) {
                if (existing.getScanActivatedAt() == null) {
                    existing.setScanActivatedAt(java.time.LocalDateTime.now());
                }
            } else {
                existing.setDepth(null);
                existing.setScanActivatedAt(null);
            }
        }
        if (existing.getScanUser() != null && existing.getScanUser() == 1) {
            existing.setDepth(normalizeDepth(request.getDepth()));
        } else if (request.getDepth() != null) {
            existing.setDepth(null);
        }
        userMapper.update(existing);
        return Optional.ofNullable(userMapper.findById(id));
    }

    public Optional<User> updateAvatar(long id, String avatarUrl) {
        User existing = userMapper.findById(id);
        if (existing == null) {
            return Optional.empty();
        }
        existing.setAvatarUrl(avatarUrl);
        userMapper.update(existing);
        return Optional.ofNullable(userMapper.findById(id));
    }

    public boolean deleteUser(long id) {
        return userMapper.delete(id) > 0;
    }

    public Optional<User> getByOpenId(String openId) {
        return Optional.ofNullable(userMapper.findByOpenId(openId));
    }

    public User getOrCreateByOpenId(String openId, String nickname, String avatarUrl) {
        return getOrCreateByOpenId(openId, nickname, avatarUrl, null);
    }

    public User getOrCreateByOpenId(String openId, String nickname, String avatarUrl, String phone) {
        User existing = userMapper.findByOpenId(openId);
        if (existing != null) {
            boolean updated = false;
            if (nickname != null && !nickname.isEmpty() && !nickname.equals(existing.getNickname())) {
                existing.setNickname(nickname);
                updated = true;
            }
            if (avatarUrl != null && !avatarUrl.isEmpty() && !avatarUrl.equals(existing.getAvatarUrl())) {
                existing.setAvatarUrl(avatarUrl);
                updated = true;
            }
            if (phone != null && !phone.isEmpty() && !phone.equals(existing.getPhone())) {
                existing.setPhone(phone);
                updated = true;
            }
            if (updated) {
                userMapper.update(existing);
            }
            return userMapper.findById(existing.getId());
        }
        User user = new User();
        user.setOpenId(openId);
        user.setNickname(nickname != null ? nickname : "微信用户");
        user.setAvatarUrl(avatarUrl);
        user.setPhone(phone);
        user.setDepth(null);
        user.setScanUser(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            User retry = userMapper.findByOpenId(openId);
            if (retry != null) {
                return retry;
            }
            throw ex;
        }
        return userMapper.findById(user.getId());
    }

    private Integer normalizeDepth(Integer depth) {
        if (depth == null) {
            return null;
        }
        if (depth < 1 || depth > 3) {
            return 1;
        }
        return depth;
    }

    private Integer normalizeScanUser(Integer scanUser) {
        if (scanUser == null) {
            return null;
        }
        return scanUser == 1 ? 1 : 0;
    }
}
