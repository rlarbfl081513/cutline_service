package com.a308.cutline.security;

import com.a308.cutline.common.entity.User;
import com.a308.cutline.domain.user.dao.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        log.info("[이벤트] >>> OAuth2 인증 성공, provider={}", registrationId);

        String email = null;
        String nickname = null;

        if ("kakao".equals(registrationId)) {
            Map<String, Object> attributes = oAuth2User.getAttributes();
            Object kakaoAccountObj = attributes.get("kakao_account");
            if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
                Object emailObj = kakaoAccount.get("email");
                if (emailObj instanceof String e) email = e;

                Object profileObj = kakaoAccount.get("profile");
                if (profileObj instanceof Map<?, ?> profile) {
                    Object nicknameObj = profile.get("nickname");
                    if (nicknameObj instanceof String n) nickname = n;
                }
            }
            if (email == null) {
                // fallback to a pseudo email using Kakao id
                Object idObj = attributes.get("id");
                String id = idObj == null ? "unknown" : String.valueOf(idObj);
                email = id + "@kakao.local";
            }
            if (nickname == null) {
                nickname = "KakaoUser";
            }
        }

        Optional<User> existing = userRepository.findByEmail(email);
        final String finalEmail = email;
        final String finalNickname = nickname;

        User user = existing.orElseGet(() ->
                userRepository.save(new User(finalEmail, finalNickname, null, null))
        );
        log.info("[이벤트] >>> 사용자 처리 완료, userId={}, email={}", user.getId(), user.getEmail());
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("email", user.getEmail());
        String token = tokenProvider.createToken(String.valueOf(user.getId()), claims);

        Cookie cookie = new Cookie("ACCESS_TOKEN", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);
        // In prod, also set cookie.setSecure(true) and SameSite attributes via response header
        response.addCookie(cookie);

        // Redirect to frontend callback
        log.info("[이벤트] >>> 프론트로 리다이렉트: {}", redirectUri);
        response.sendRedirect(redirectUri);
    }
}


