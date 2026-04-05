package ru.vladmikhayl.integrations.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.vladmikhayl.integrations.dto.request.TelegramSendMessageRequest;

@Profile("!test")
@FeignClient(name = "telegramBotClient", url = "${telegram.bot-api-url}")
public interface TelegramBotClient {
    @PostMapping("/sendMessage")
    void sendMessage(@RequestBody TelegramSendMessageRequest request);
}
