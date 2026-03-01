package ru.vladmikhayl.task_management.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Информация о задаче")
public class TaskResponse {
    @Schema(description = "ID задачи")
    private UUID id;

    @Schema(description = "ID списка дел")
    private UUID listId;

    @Schema(description = "Название задачи", example = "Вынести мусор")
    private String title;

    @Schema(description = "Тип повторения задачи", example = "WeeklyByDays")
    private RecurrenceType recurrenceType;

    @Schema(description = "Интервал в днях для recurrenceType = EveryNdays", example = "3", nullable = true)
    private Integer intervalDays;

    @Schema(description = "Битовая маска выбранных дней для recurrenceType = WeeklyByDays", example = "21", nullable = true)
    private Integer weekdaysMask;

    @Schema(description = "Выбранные дни недели (0=Пн..6=Вс) для recurrenceType = WeeklyByDays", example = "[0,2,4]", nullable = true)
    private Set<Integer> weekdays;

    @Schema(description = "Тип назначения исполнителя", example = "RoundRobin")
    private AssignmentType assignmentType;

    @Schema(description = "ID фиксированного исполнителя для assignmentType = FixedUser", nullable = true)
    private UUID fixedUserId;

    @Schema(description = "Кандидаты для assignmentType = RoundRobin", nullable = true)
    private List<TodoListMemberResponse> roundRobinUsers;

    @Schema(description = "Исполнитель по дням недели для assignmentType = ByWeekday", nullable = true)
    private Map<Integer, UUID> weekdayAssignees;

    @Schema(description = "Текущий курсор для RoundRobin", example = "0", nullable = true)
    private Integer rrCursor;
}
