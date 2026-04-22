package com.ssafya408.cutlineparsing.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        log.info("[ JWT 초기화 ] >>> 토큰 유효시간: {}ms", accessTokenValidityMs);
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
        log.info("[ JWT 생성 ] >>> 사용자: {}, 만료: {}", subject, Date.from(expiry));
        return token;
    }

    public Claims parseClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            return claims;
        } catch (Exception e) {
            log.error("[ JWT 파싱 실패 ] >>> {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     * @throws RuntimeException 토큰이 유효하지 않은 경우
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            Object uid = claims.get("uid");
            if (uid instanceof Integer) {
                return ((Integer) uid).longValue();
            } else if (uid instanceof Long) {
                return (Long) uid;
            } else if (uid instanceof String) {
                return Long.parseLong((String) uid);
            } else {
                throw new RuntimeException("토큰에서 유효한 사용자 ID를 찾을 수 없습니다");
            }
        } catch (Exception e) {
            log.error("[ JWT 사용자 ID 추출 실패 ] >>> {}", e.getMessage());
            throw new RuntimeException("토큰에서 사용자 ID 추출 실패", e);
        }
    }

    /**
     * JWT 토큰에서 사용자 이메일 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 이메일
     * @throws RuntimeException 토큰이 유효하지 않은 경우
     */
    public String getUserEmailFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String email = (String) claims.get("email");
            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("토큰에서 유효한 이메일을 찾을 수 없습니다");
            }
            return email;
        } catch (Exception e) {
            log.error("[ JWT 이메일 추출 실패 ] >>> {}", e.getMessage());
            throw new RuntimeException("토큰에서 이메일 추출 실패", e);
        }
    }
}


