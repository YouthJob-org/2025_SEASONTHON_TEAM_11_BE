package com.youthjob.api.auth.jwt;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {
    private final Map<String, Instant> map = new ConcurrentHashMap<>();

    public void add(String token, Instant expiresAt) {
        map.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        Instant exp = map.get(token);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) { map.remove(token); return false; }
        return true;
    }
}
