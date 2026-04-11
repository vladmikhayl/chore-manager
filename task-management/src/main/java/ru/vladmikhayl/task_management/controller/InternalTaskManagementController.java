package ru.vladmikhayl.task_management.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladmikhayl.task_management.dto.request.TasksForUsersRequest;
import ru.vladmikhayl.task_management.dto.response.UserTasksForReminderResponse;
import ru.vladmikhayl.task_management.service.InternalTaskManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Hidden
public class InternalTaskManagementController {
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
}
