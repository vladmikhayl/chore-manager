package ru.vladmikhayl.task_management.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Приглашение в список дел")
public class CreateInviteResponse {
    @Schema(description = "Токен приглашения", example = "7fbd5f0f-7f34-4d0d-9b6a-c1d1e1c2f3a4")
    private String token;

    @Schema(description = "Дата и время истечения приглашения (UTC)")
    private Instant expiresAt;
}
