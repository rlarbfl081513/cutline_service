package com.ssafya408.cutlineparsing.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * JWT 기반 인증 정보
 * 
 * <p>Spring Security의 Authentication 인터페이스를 구현하여
 * JWT 토큰에서 추출한 사용자 정보를 SecurityContext에 저장합니다.</p>
 * 
 * <h3>포함 정보:</h3>
 * <ul>
 *   <li><b>사용자 ID:</b> 데이터베이스 Primary Key</li>
 *   <li><b>사용자 이름:</b> 표시용 이름</li>
 *   <li><b>이메일:</b> 사용자 이메일</li>
 *   <li><b>JWT 토큰:</b> 원본 토큰 문자열</li>
 * </ul>
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class JwtAuthentication extends AbstractAuthenticationToken {

    private final Long userId;
    private final String userName;
    private final String email;
    private final String token;

    /**
     * JWT 인증 정보 생성자
     * 
     * @param userId 사용자 ID
     * @param userName 사용자 이름
     * @param email 사용자 이메일
     * @param token JWT 토큰 문자열
     */
    public JwtAuthentication(Long userId, String userName, String email, String token) {
        super(Collections.emptyList()); // 현재 권한 시스템 사용 안함
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.token = token;
        setAuthenticated(true); // 인증됨으로 설정
    }

    /**
     * 인증 자격 증명 (JWT 토큰)
     */
    @Override
    public Object getCredentials() {
        return token;
    }

    /**
     * 사용자 주체 (사용자 ID)
     */
    @Override
    public Object getPrincipal() {
        return userId;
    }

    /**
     * 사용자 ID 반환
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 사용자 이름 반환
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 사용자 이메일 반환
     */
    public String getEmail() {
        return email;
    }

    /**
     * JWT 토큰 반환
     */
    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "JwtAuthentication{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", authenticated=" + isAuthenticated() +
                '}';
    }
}




