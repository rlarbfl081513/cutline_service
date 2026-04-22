package com.ssafya408.cutlineparsing.config;


import com.ssafya408.cutlineparsing.common.security.JwtAuthenticationFilter;
import com.ssafya408.cutlineparsing.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[이벤트] >>> SecurityFilterChain 초기화 시작");
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
//                .requestMatchers(
//                    "/", "/error", "/ping", "/actuator/**",
//                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
//                    "/login/**", "/oauth2/**", "/auth/**"
//                ).permitAll()
//                .requestMatchers(HttpMethod.GET, "/naver/**").permitAll()
//                .anyRequest().authenticated()
                            .anyRequest().permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .deleteCookies("ACCESS_TOKEN")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
            );

        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        SecurityFilterChain chain = http.build();
        log.info("[이벤트] >>> SecurityFilterChain 초기화 완료");
        return chain;
    }
}


