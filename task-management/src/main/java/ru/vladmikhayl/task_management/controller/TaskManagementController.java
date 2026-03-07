package ru.vladmikhayl.task_management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladmikhayl.task_management.dto.request.AcceptInviteRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTaskRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.request.UpdateAssignmentRuleRequest;
import ru.vladmikhayl.task_management.dto.response.*;
import ru.vladmikhayl.task_management.service.TaskManagementService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
        @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
})
public class TaskManagementController {
    private final TaskManagementService taskManagementService;

    @GetMapping("/lists")
    @Operation(summary = "Получить списки дел, в которых состоит текущий пользователь")
    @ApiResponse(responseCode = "200", description = "Успешно")
    public ResponseEntity<List<TodoListShortResponse>> getLists(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId
    ) {
        return ResponseEntity.ok(taskManagementService.getLists(userId));
    }

    @PostMapping("/lists")
    @Operation(summary = "Создать новый список дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Список создан"),
            @ApiResponse(responseCode = "409", description = "У текущего пользователя уже есть список с таким названием", content = @Content)
    })
    public ResponseEntity<Void> createList(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @Valid @RequestBody CreateTodoListRequest request
    ) {
        taskManagementService.createList(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/lists/{listId}/invites")
    @Operation(summary = "Создать приглашение для вступления в список дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Приглашение создано"),
            @ApiResponse(responseCode = "403", description = "Только создатель списка может создавать приглашения", content = @Content),
            @ApiResponse(responseCode = "404", description = "Список дел не найден", content = @Content)
    })
    public ResponseEntity<CreateInviteResponse> createInvite(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID listId
    ) {
        var invite = taskManagementService.createInvite(userId, listId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invite);
    }

    @PostMapping("/invites/accept")
    @Operation(summary = "Принять приглашение в список дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "400", description = "Приглашение истекло, или переданы некорректные параметры или тело", content = @Content),
            @ApiResponse(responseCode = "404", description = "Приглашение или список не найдены", content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь уже состоит в списке", content = @Content)
    })
    public ResponseEntity<Void> acceptInvite(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @Valid @RequestBody AcceptInviteRequest request
    ) {
        taskManagementService.acceptInvite(userId, request.getToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/lists/{listId}")
    @Operation(summary = "Получить общую информацию о списке дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Список дел не найден", content = @Content)
    })
    public ResponseEntity<TodoListDetailsResponse> getListDetails(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID listId
    ) {
        return ResponseEntity.ok(taskManagementService.getListDetails(userId, listId));
    }

    @PostMapping("/lists/{listId}/tasks")
    @Operation(summary = "Создать задачу в списке дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Задача создана"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Список дел не найден", content = @Content),
            @ApiResponse(responseCode = "409", description = "В списке уже есть задача с таким названием", content = @Content)
    })
    public ResponseEntity<UUID> createTask(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID listId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        var created = taskManagementService.createTask(userId, listId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/lists/{listId}/tasks")
    @Operation(summary = "Получить список задач в списке дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Список дел не найден", content = @Content)
    })
    public ResponseEntity<List<TaskResponse>> getTasks(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID listId
    ) {
        return ResponseEntity.ok(taskManagementService.getTasks(userId, listId));
    }

    @PutMapping("/tasks/{taskId}/assignment-rule")
    @Operation(summary = "Изменить правило назначения исполнителя задачи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content)
    })
    public ResponseEntity<Void> updateAssignmentRule(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateAssignmentRuleRequest request
    ) {
        taskManagementService.updateAssignmentRule(userId, taskId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Удалить задачу")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача удалена"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content)
    })
    public ResponseEntity<Void> deleteTask(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID taskId
    ) {
        taskManagementService.deleteTask(userId, taskId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/lists/{listId}/members/me")
    @Operation(summary = "Выйти из списка дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Нельзя выйти из списка", content = @Content),
            @ApiResponse(responseCode = "404", description = "Список не найден", content = @Content)
    })
    public ResponseEntity<Void> leaveList(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID listId
    ) {
        taskManagementService.leaveList(userId, listId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/lists/{listId}")
    @Operation(summary = "Удалить список дел")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список удалён"),
            @ApiResponse(responseCode = "403", description = "Удалять список может только создатель", content = @Content),
            @ApiResponse(responseCode = "404", description = "Список не найден", content = @Content)
    })
    public ResponseEntity<Void> deleteList(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID listId
    ) {
        taskManagementService.deleteList(userId, listId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/tasks/{taskId}/completions/{date}")
    @Operation(summary = "Отметить задачу выполненной за дату")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача отмечена выполненной"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content)
    })
    public ResponseEntity<Void> completeTask(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID taskId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        taskManagementService.completeTask(userId, taskId, date);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tasks/{taskId}/completions/{date}")
    @Operation(summary = "Получить статус выполнения задачи за указанную дату")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content)
    })
    public ResponseEntity<TaskCompletionStatusResponse> getTaskCompletion(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID taskId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(taskManagementService.getTaskCompletion(userId, taskId, date));
    }

    @DeleteMapping("/tasks/{taskId}/completions/{date}")
    @Operation(summary = "Снять отметку выполнения задачи за дату")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отметка выполнения удалена"),
            @ApiResponse(responseCode = "403", description = "Пользователь не состоит в списке", content = @Content),
            @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content)
    })
    public ResponseEntity<Void> deleteTaskCompletion(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @PathVariable UUID taskId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        taskManagementService.deleteTaskCompletion(userId, taskId, date);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tasks")
    @Operation(summary = "Получить задачи текущего пользователя на выбранный день")
    @ApiResponse(responseCode = "200", description = "Успешно")
    public ResponseEntity<List<TaskResponse>> getTasksForDay(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(taskManagementService.getTasksForDay(userId, date));
    }
}
