package com.a308.cutline.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "spring.naver.shopping")
@Getter
@Setter
public class NaverShoppingConfig {
    private String clientId;
    private String clientSecret;
    private String apiUrl;

    @Bean
    public WebClient naverShoppingWebClient() {
        return WebClient.builder()
                .baseUrl(this.apiUrl)
                .defaultHeader("X-Naver-Client-Id", this.clientId)
                .defaultHeader("X-Naver-Client-Secret", this.clientSecret)
                .build();
    }
}