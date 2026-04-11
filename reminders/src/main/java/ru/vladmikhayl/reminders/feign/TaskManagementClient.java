package ru.vladmikhayl.reminders.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.vladmikhayl.reminders.config.TaskManagementFeignConfig;
import ru.vladmikhayl.reminders.dto.request.TasksForUsersRequest;
import ru.vladmikhayl.reminders.dto.response.UserTasksForReminderResponse;

import java.util.List;

@Profile("!test")
@FeignClient(name = "gateway", configuration = TaskManagementFeignConfig.class)
public interface TaskManagementClient {
    @PostMapping("/api/v1/internal/tasks-for-users")
    List<UserTasksForReminderResponse> getTasksForUsers(
            @RequestBody TasksForUsersRequest request
    );
}
