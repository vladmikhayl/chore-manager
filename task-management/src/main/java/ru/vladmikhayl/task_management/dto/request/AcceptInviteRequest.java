package ru.vladmikhayl.task_management.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на принятие приглашения в список дел")
public class AcceptInviteRequest {
    @NotBlank(message = "Токен приглашения не может быть пустым")
    @Schema(description = "Токен приглашения", example = "7fbd5f0f-7f34-4d0d-9b6a-c1d1e1c2f3a4")
    private String token;
}
