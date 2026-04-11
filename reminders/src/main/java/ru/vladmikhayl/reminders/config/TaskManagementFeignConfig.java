package ru.vladmikhayl.reminders.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskManagementFeignConfig {
    @Bean
    public RequestInterceptor internalTokenInterceptor(
            @Value("${security.internal.token}") String token
    ) {
        return template -> template.header("X-Internal-Token", token);
    }
}
