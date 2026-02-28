package ru.vladmikhayl.task_management.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.vladmikhayl.task_management.config.IdentityFeignConfig;

import java.util.UUID;

@Profile("!test")
@FeignClient(name = "gateway", configuration = IdentityFeignConfig.class)
public interface IdentityClient {
    @GetMapping("/api/v1/internal/users/{userId}/login")
    ResponseEntity<String> getUserLogin(@PathVariable UUID userId);
}
