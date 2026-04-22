package com.a308.cutline.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;

    public JwtTokenProvider(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-token-validity-ms}") long accessTokenValidityMs
    ) {
        // Support for raw string or base64
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes();
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityMs = accessTokenValidityMs;
        log.info("[이벤트] >>> JwtTokenProvider 초기화, accessTokenValidityMs={}", accessTokenValidityMs);
    }

    public String createToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenValidityMs);
        String token = Jwts.builder()
            // setClaims overwrites existing claims, so call it BEFORE setSubject
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
        log.info("[이벤트] >>> JWT 생성 완료, subject={}, expiresAt={}", subject, Date.from(expiry));
        return token;
    }

    public Claims parseClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            log.info("[이벤트] >>> JWT 파싱 성공, subject={}", claims.getSubject());
            return claims;
        } catch (Exception e) {
            log.error("[이벤트] >>> JWT 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }
}


