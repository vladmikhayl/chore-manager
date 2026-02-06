package ru.taskmanager.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на аутентификацию")
public class LoginRequest {
    @Schema(
            description = "Логин",
            example = "user",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String login;

    @Schema(
            description = "Пароль",
            example = "password",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;
}
