package ru.vladmikhayl.integrations.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Webhook-запрос от Telegram")
public class TelegramWebhookRequest {
    @Schema(description = "Сообщение из Telegram")
    private TelegramMessage message;

    @Data
    @Schema(description = "Сообщение из Telegram")
    public static class TelegramMessage {
        @Schema(description = "Текст сообщения", example = "/start abcdef123456")
        private String text;

        @Schema(description = "Чат, из которого пришло сообщение")
        private TelegramChat chat;
    }

    @Data
    @Schema(description = "Информация о чате Telegram")
    public static class TelegramChat {
        @Schema(description = "Идентификатор чата Telegram", example = "123456789")
        private Long id;
    }
}
