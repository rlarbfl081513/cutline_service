package com.ssafya408.cutlineparsing.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveTokenFromCookie(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = tokenProvider.parseClaims(token);
                String subject = claims.getSubject();
                if (!StringUtils.hasText(subject)) {
                    throw new IllegalArgumentException("Empty JWT subject");
                }
                log.info("[이벤트] >>> JWT 인증 성공, subject={}", subject);

                User principal = new User(subject, "", Collections.emptyList());
                Authentication authentication = new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
                ((UsernamePasswordAuthenticationToken) authentication).setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("[이벤트] >>> JWT 인증 실패: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("ACCESS_TOKEN".equals(cookie.getName())) {
                log.info("[이벤트] >>> ACCESS_TOKEN 쿠키 발견");
                return cookie.getValue();
            }
        }
        log.info("[이벤트] >>> ACCESS_TOKEN 쿠키 없음");
        return null;
    }
}


