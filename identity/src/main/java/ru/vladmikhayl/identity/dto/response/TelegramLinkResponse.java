package ru.vladmikhayl.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о привязке Telegram-аккаунта")
public class TelegramLinkResponse {
    @Schema(description = "Привязан ли Telegram", example = "true")
    private boolean linked;

    @Schema(description = "Идентификатор Telegram-чата, если аккаунт привязан", example = "123456789")
    private Long chatId;
}
