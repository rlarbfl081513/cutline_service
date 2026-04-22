package com.ssafya408.cutlineparsing.common.security;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 인증 관련 유틸리티 클래스
 */
public class AuthenticationUtils {

  /**
   * 현재 인증된 사용자의 ID를 반환합니다.
   * @return 현재 사용자의 ID
   * @throws IllegalStateException 인증되지 않은 경우
   */
  public static Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
      throw new IllegalStateException("인증되지 않은 사용자입니다");
    }

    try {
      return Long.valueOf(authentication.getName());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("유효하지 않은 사용자 ID 형식입니다");
    }
  }

  /**
   * 현재 인증된 사용자의 ID를 안전하게 반환합니다.
   * @return 현재 사용자의 ID (인증되지 않은 경우 null)
   */
  public static Long getCurrentUserIdSafe() {
    try {
      return getCurrentUserId();
    } catch (IllegalStateException e) {
      return null;
    }
  }
}