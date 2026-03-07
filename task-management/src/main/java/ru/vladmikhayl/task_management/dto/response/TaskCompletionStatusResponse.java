package ru.vladmikhayl.task_management.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Статус выполнения задачи за указанную дату")
public class TaskCompletionStatusResponse {
    @Schema(description = "Дата")
    private LocalDate date;

    @Schema(description = "Выполнена ли задача за эту дату")
    private boolean completed;

    @Schema(description = "ID пользователя, который отметил задачу выполненной")
    private UUID completedByUserId;

    @Schema(description = "Логин пользователя, который отметил задачу выполненной")
    private String completedByLogin;

    @Schema(description = "Дата и время отметки выполнения")
    private LocalDateTime completedAt;
}
