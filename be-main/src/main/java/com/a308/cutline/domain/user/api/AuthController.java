package com.a308.cutline.domain.user.api;

import com.a308.cutline.common.dto.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> me(Authentication authentication) {
        if (authentication != null) {
            log.info("[이벤트] >>> /auth/me 요청, principal={}", authentication.getPrincipal());
        } else {
            log.info("[이벤트] >>> /auth/me 요청, 인증 없음");
        }
        return ResponseEntity.ok(ApiResponse.success(authentication != null ? authentication.getPrincipal() : null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletResponse response) {
        log.info("[이벤트] >>> /auth/logout 요청, ACCESS_TOKEN 쿠키 제거");
        Cookie cookie = new Cookie("ACCESS_TOKEN", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(ApiResponse.success("logged out"));
    }
}


