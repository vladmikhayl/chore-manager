package ru.vladmikhayl.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Ответ на аутентификацию")
public class LoginResponse {
    @Schema(
            description = "Выданный JWT",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4Y29tdmxhZCIsImp0aSI6IjEiLCJpYXQiOjE3NDMxOTM1MzQsImV4cCI6MTc0MzI3OTkzNH0.jxYFAzVRy7qgIsLgYmqJNDl6_wleYDrWujGNJoNrDqw"
    )
    private String token;
}
