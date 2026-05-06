package ru.vladmikhayl.task_management.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladmikhayl.task_management.dto.request.TasksForUsersRequest;
import ru.vladmikhayl.task_management.dto.response.TaskResponseShort;
import ru.vladmikhayl.task_management.dto.response.UserTasksForReminderResponse;
import ru.vladmikhayl.task_management.service.InternalTaskManagementService;
import ru.vladmikhayl.task_management.service.TaskManagementService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Hidden
public class InternalTaskManagementController {
    private final TaskManagementService taskManagementService;
    private final InternalTaskManagementService internalTaskManagementService;

    @PostMapping("/tasks-for-users")
    public ResponseEntity<List<UserTasksForReminderResponse>> getTasksForUsers(
            @RequestBody TasksForUsersRequest request
    ) {
        var result = internalTaskManagementService.getTasksForUsers(
                request.getUserIds(),
                request.getDate()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponseShort>> getTasksForDay(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TaskResponseShort> response = taskManagementService.getTasksForDay(userId, date)
                .stream()
                .map(task -> TaskResponseShort.builder()
                        .id(task.getId())
                        .listTitle(task.getListTitle())
                        .title(task.getTitle())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/tasks/{taskId}/completions/{date}")
    public ResponseEntity<Void> completeTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        taskManagementService.completeTask(userId, taskId, date);
        return ResponseEntity.ok().build();
    }
}
