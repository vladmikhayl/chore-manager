package ru.vladmikhayl.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@Schema(description = "Ответ с данными профиля пользователя")
public class ProfileResponse {
    @Schema(description = "Логин пользователя", example = "vlad")
    private String login;

    @Schema(description = "Включены ли напоминания", example = "true")
    private boolean dailyReminderEnabled;

    @Schema(description = "Время получения напоминаний", example = "10:00")
    private LocalTime dailyReminderTime;
}
