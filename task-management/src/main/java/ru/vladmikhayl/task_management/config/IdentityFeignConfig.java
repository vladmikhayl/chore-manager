package ru.vladmikhayl.task_management.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityFeignConfig {
    @Bean
    public RequestInterceptor internalTokenInterceptor(
            @Value("${security.internal.token}") String token
    ) {
        return template -> template.header("X-Internal-Token", token);
    }
}
