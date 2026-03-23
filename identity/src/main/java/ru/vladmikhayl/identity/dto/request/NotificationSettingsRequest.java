package ru.vladmikhayl.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на изменение настроек напоминаний")
public class NotificationSettingsRequest {
    @Schema(
            description = "Включены ли напоминания (новое значение)",
            example = "true"
    )
    private Boolean dailyReminderEnabled; // null = не менять

    @Schema(
            description = "Время получения напоминаний (новое значение)",
            example = "10:00"
    )
    private LocalTime dailyReminderTime; // null = не менять
}
