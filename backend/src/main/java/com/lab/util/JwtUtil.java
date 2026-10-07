package com.lab.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {

    /** B4：登出失效 token 黑名单（单机内存版；分布式部署可替换为 Redis） */
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    @Value("${lab.jwt-secret:lab-equipment-system-jwt-secret-2026}")
    private String secret;

    @Value("${lab.jwt-expire-hours:24}")
    private long expireHours;

    public String create(Long userId, String username, Long roleId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireHours * 3600 * 1000);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("roleId", roleId)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }

    /** 将 token 加入黑名单（登出） */
    public void blacklist(String token) {
        if (token != null && !token.isEmpty()) {
            blacklist.add(token);
        }
    }

    /** token 是否在黑名单中 */
    public boolean isBlacklisted(String token) {
        return token != null && blacklist.contains(token);
    }
}
