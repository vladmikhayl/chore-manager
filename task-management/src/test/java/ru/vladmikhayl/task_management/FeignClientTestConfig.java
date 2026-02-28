package ru.vladmikhayl.task_management;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import ru.vladmikhayl.task_management.feign.IdentityClient;

@TestConfiguration
public class FeignClientTestConfig {
    @Bean
    public IdentityClient identityClient() {
        return Mockito.mock(IdentityClient.class);
    }
}
