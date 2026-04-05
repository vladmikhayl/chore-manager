package ru.vladmikhayl.integrations.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vladmikhayl.integrations.dto.request.TelegramWebhookRequest;
import ru.vladmikhayl.integrations.service.IntegrationsService;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "Вебхуки для интеграций")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Вебхук успешно обработан"),
        @ApiResponse(responseCode = "400", description = "Передано некорректное тело запроса", content = @Content)
})
public class IntegrationsController {
    private final IntegrationsService integrationsService;

    @PostMapping("/telegram/webhook")
    @Operation(summary = "Обработать вебхук от Telegram")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody TelegramWebhookRequest request
    ) {
        integrationsService.handleWebhook(request);
        return ResponseEntity.ok().build();
    }
}
