package ru.taskmanager.identity.dto.request;

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
public class NotificationSettingsRequest {
    private Boolean dailyReminderEnabled; // null = не менять

    private LocalTime dailyReminderTime; // null = не менять

    @Min(value = -12, message = "Часовой пояс не должен быть меньше -12")
    @Max(value = 14, message = "Часовой пояс не должен быть больше 14")
    private Integer timezoneOffsetHours; // null = не менять
}
