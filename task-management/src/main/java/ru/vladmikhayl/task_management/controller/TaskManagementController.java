package ru.vladmikhayl.task_management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.response.TodoListShortResponse;
import ru.vladmikhayl.task_management.service.TaskManagementService;

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
}
