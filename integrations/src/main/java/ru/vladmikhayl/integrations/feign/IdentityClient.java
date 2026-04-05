package ru.vladmikhayl.integrations.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.vladmikhayl.integrations.config.IdentityFeignConfig;
import ru.vladmikhayl.integrations.dto.request.TelegramLinkRequest;

@Profile("!test")
@FeignClient(name = "gateway", configuration = IdentityFeignConfig.class)
public interface IdentityClient {
    @PostMapping("/api/v1/internal/telegram/link")
    ResponseEntity<Void> linkTelegramAccount(@RequestBody TelegramLinkRequest request);
}
