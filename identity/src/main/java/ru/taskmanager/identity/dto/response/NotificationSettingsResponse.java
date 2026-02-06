package ru.taskmanager.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@Schema(description = "Ответ на получение настроек напоминаний")
public class NotificationSettingsResponse {
    @Schema(
            description = "Включены ли напоминания",
            example = "true"
    )
    private boolean dailyReminderEnabled;

    @Schema(
            description = "Время получения напоминаний",
            example = "10:00"
    )
    private LocalTime dailyReminderTime;

    @Schema(
            description = "Сдвиг по часовому поясу",
            example = "3"
    )
    private int timezoneOffsetHours;
}
