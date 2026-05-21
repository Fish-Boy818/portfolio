package com.untitled.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserAuthService {
    private static final long TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1000;
    private final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();

    public String issueToken(long userId) {
        String token = "utk-" + UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(userId, System.currentTimeMillis()));
        return token;
    }

    public Long resolveUserId(String authorization) {
        String token = parseToken(authorization);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - session.issuedAt > TOKEN_TTL_MS) {
            sessions.remove(token);
            return null;
        }
        return session.userId;
    }

    public void revoke(String authorization) {
        String token = parseToken(authorization);
        if (StringUtils.hasText(token)) {
            sessions.remove(token);
        }
    }

    private String parseToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return "";
        }
        String token = authorization.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        return token;
    }

    private static class Session {
        private final long userId;
        private final long issuedAt;

        private Session(long userId, long issuedAt) {
            this.userId = userId;
            this.issuedAt = issuedAt;
        }
    }
}
