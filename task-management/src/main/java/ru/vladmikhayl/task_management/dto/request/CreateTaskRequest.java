package ru.vladmikhayl.task_management.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vladmikhayl.task_management.entity.AssignmentType;
import ru.vladmikhayl.task_management.entity.RecurrenceType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Запрос на создание задачи")
public class CreateTaskRequest {
    @NotBlank(message = "Название задачи не может быть пустым.")
    @Size(max = 255, message = "Название задачи не может быть длиннее 255 символов.")
    @Schema(description = "Название задачи", example = "Вынести мусор")
    private String title;

    @NotNull(message = "Не указано правило повторения.")
    @Schema(description = "Тип повторения задачи", example = "WeeklyByDays")
    private RecurrenceType recurrenceType;

    @Min(value = 1, message = "Интервал в днях должен быть >= 1.")
    @Schema(description = "Интервал в днях для recurrenceType = EveryNdays", example = "3", nullable = true)
    private Integer intervalDays;

    @Schema(description = "Дни недели для recurrenceType = WeeklyByDays", example = "[0,2,4]", nullable = true)
    private Set<
            @Min(value = 0, message = "День недели должен быть в диапазоне 0..6")
            @Max(value = 6, message = "День недели должен быть в диапазоне 0..6")
                    Integer> weekdays;

    @NotNull(message = "Не указано правило назначения исполнителя.")
    @Schema(description = "Тип назначения исполнителя", example = "RoundRobin")
    private AssignmentType assignmentType;

    @Schema(description = "ID фиксированного исполнителя для assignmentType = FixedUser", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", nullable = true)
    private UUID fixedUserId;

    @Schema(description = "Кандидаты для assignmentType = RoundRobin", example = " [\"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"11111111-2222-3333-4444-555555555555\"]", nullable = true)
    private List<UUID> roundRobinUserIds;

    @Schema(
            description = "Исполнитель по дням недели для assignmentType = ByWeekday",
            example = """
                    {"0":"3fa85f64-5717-4562-b3fc-2c963f66afa6",
                     "1":"11111111-2222-3333-4444-555555555555",
                     "2":"3fa85f64-5717-4562-b3fc-2c963f66afa6",
                     "3":"11111111-2222-3333-4444-555555555555",
                     "4":"3fa85f64-5717-4562-b3fc-2c963f66afa6",
                     "5":"11111111-2222-3333-4444-555555555555",
                     "6":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}
                    """,
            nullable = true
    )
    private Map<
            @Min(value = 0, message = "День недели должен быть в диапазоне 0..6")
            @Max(value = 6, message = "День недели должен быть в диапазоне 0..6")
                    Integer, UUID> weekdayAssignees;
}
