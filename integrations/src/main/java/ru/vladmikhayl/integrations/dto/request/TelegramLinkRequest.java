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
@Schema(description = "Внутренний запрос на привязку Telegram-аккаунта к пользователю")
public class TelegramLinkRequest {
    @Schema(description = "Одноразовый токен привязки Telegram", example = "abcdef1234567890")
    private String token;

    @Schema(description = "Идентификатор чата Telegram", example = "123456789")
    private Long chatId;
}
