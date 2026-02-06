package ru.taskmanager.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = -12, message = "Часовой пояс не должен быть меньше -12")
    @Max(value = 14, message = "Часовой пояс не должен быть больше 14")
    @Schema(
            description = "Сдвиг по часовому поясу (новое значение)",
            example = "3"
    )
    private Integer timezoneOffsetHours; // null = не менять
}
