package ru.vladmikhayl.task_management.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Участник списка дел")
public class TodoListMemberResponse {
    @Schema(description = "Идентификатор пользователя")
    private UUID userId;

    @Schema(
            description = "Логин пользователя",
            example = "user1"
    )
    private String login;
}
