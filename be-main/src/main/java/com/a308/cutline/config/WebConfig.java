package com.a308.cutline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS는 nginx에서 처리하므로 Spring Boot CORS 설정 비활성화
    
    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/**")
    //             .allowedOriginPatterns("*") // 모든 오리진 허용
    //             .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
    //             .allowedHeaders("*")
    //             .allowCredentials(true)
    //             .maxAge(3600); // preflight 캐시 시간 (1시간)
    // }

    // @Bean
    // public CorsConfigurationSource corsConfigurationSource() {
    //     CorsConfiguration configuration = new CorsConfiguration();
    //     
    //     // 모든 오리진 허용
    //     configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    //     
    //     // 허용할 HTTP 메소드
    //     configuration.setAllowedMethods(Arrays.asList(
    //         "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    //     ));
    //     
    //     // 허용할 헤더
    //     configuration.setAllowedHeaders(Arrays.asList("*"));
    //     
    //     // 인증 정보 허용
    //     configuration.setAllowCredentials(true);
    //     
    //     // preflight 요청 캐시 시간
    //     configuration.setMaxAge(3600L);
    //     
    //     // 클라이언트에서 접근할 수 있는 응답 헤더
    //     configuration.setExposedHeaders(Arrays.asList(
    //         "Content-Length", "Content-Range", "Content-Type", "Authorization"
    //     ));
    //     
    //     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //     source.registerCorsConfiguration("/**", configuration);
    //     
    //     return source;
    // }
}
