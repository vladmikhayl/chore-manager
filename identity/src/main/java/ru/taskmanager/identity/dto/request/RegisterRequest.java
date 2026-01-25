package ru.taskmanager.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = 3, max = 30, message = "Логин должен содержать от 3 до 30 символов")
    private String login;

    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 5, max = 72, message = "Пароль должен содержать от 5 до 72 символов")
    private String password;
}
