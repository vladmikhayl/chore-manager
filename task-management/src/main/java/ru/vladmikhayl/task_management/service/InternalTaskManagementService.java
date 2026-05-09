package ru.vladmikhayl.task_management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vladmikhayl.task_management.dto.response.ReminderTaskResponse;
import ru.vladmikhayl.task_management.dto.response.UserTasksForReminderResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalTaskManagementService {
    private final TaskManagementService taskManagementService;

    public List<UserTasksForReminderResponse> getTasksForUsers(
            List<UUID> userIds,
            LocalDate date
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userIds.stream()
                .map(userId -> {
                    var tasks = taskManagementService.getTasksForDay(userId, date);

                    var mapped = tasks.stream()
                            .map(task -> ReminderTaskResponse.builder()
                                    .taskId(task.getId())
                                    .title(task.getTitle())
                                    .listTitle(task.getListTitle())
                                    .completed(task.isCompleted())
                                    .build()
                            )
                            .toList();

                    return UserTasksForReminderResponse.builder()
                            .userId(userId)
                            .tasks(mapped)
                            .build();
                })
                .toList();
    }
}
