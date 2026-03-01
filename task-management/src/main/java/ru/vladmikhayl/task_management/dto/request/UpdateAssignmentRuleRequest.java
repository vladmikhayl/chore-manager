package ru.vladmikhayl.task_management.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vladmikhayl.task_management.entity.AssignmentType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Запрос на изменение правила назначения исполнителя задачи")
public class UpdateAssignmentRuleRequest {
    @NotNull(message = "Не указано правило назначения исполнителя")
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
