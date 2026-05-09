package ru.vladmikhayl.task_management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladmikhayl.task_management.dto.response.ReminderTaskResponse;
import ru.vladmikhayl.task_management.dto.response.TaskResponse;
import ru.vladmikhayl.task_management.dto.response.UserTasksForReminderResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InternalTaskManagementServiceTest {
    @Mock
    private TaskManagementService taskManagementService;

    @InjectMocks
    private InternalTaskManagementService internalTaskManagementService;

    @Test
    void getTasksForUsers_singleUserWithTasks_returnsMappedTasks() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        TaskResponse task1 = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Task 1")
                .listTitle("List 1")
                .completed(false)
                .build();

        TaskResponse task2 = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Task 2")
                .listTitle("List 2")
                .completed(false)
                .build();

        when(taskManagementService.getTasksForDay(userId, date))
                .thenReturn(List.of(task1, task2));

        var result = internalTaskManagementService.getTasksForUsers(
                List.of(userId),
                date
        );

        assertThat(result).hasSize(1);

        var userResult = result.get(0);
        assertThat(userResult.getUserId()).isEqualTo(userId);
        assertThat(userResult.getTasks()).hasSize(2);

        assertThat(userResult.getTasks())
                .extracting(ReminderTaskResponse::getTitle, ReminderTaskResponse::isCompleted)
                .containsExactly(
                        tuple("Task 1", false),
                        tuple("Task 2", false)
                );
    }

    @Test
    void getTasksForUsers_completedTasks_returnsWithCompletedFlag() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        TaskResponse completedTask = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Done task")
                .listTitle("List")
                .completed(true)
                .build();

        TaskResponse activeTask = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Active task")
                .listTitle("List")
                .completed(false)
                .build();

        when(taskManagementService.getTasksForDay(userId, date))
                .thenReturn(List.of(completedTask, activeTask));

        var result = internalTaskManagementService.getTasksForUsers(
                List.of(userId),
                date
        );

        var tasks = result.get(0).getTasks();

        assertThat(tasks).hasSize(2);

        assertThat(tasks)
                .extracting(ReminderTaskResponse::getTitle, ReminderTaskResponse::isCompleted)
                .containsExactly(
                        tuple("Done task", true),
                        tuple("Active task", false)
                );
    }

    @Test
    void getTasksForUsers_noTasks_returnsEmptyListForUser() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        when(taskManagementService.getTasksForDay(userId, date))
                .thenReturn(List.of());

        var result = internalTaskManagementService.getTasksForUsers(
                List.of(userId),
                date
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTasks()).isEmpty();
    }

    @Test
    void getTasksForUsers_multipleUsers_returnsResultsForEachUser() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        when(taskManagementService.getTasksForDay(user1, date))
                .thenReturn(List.of());

        TaskResponse task = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Task")
                .listTitle("List")
                .completed(false)
                .build();

        when(taskManagementService.getTasksForDay(user2, date))
                .thenReturn(List.of(task));

        var result = internalTaskManagementService.getTasksForUsers(
                List.of(user1, user2),
                date
        );

        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(UserTasksForReminderResponse::getUserId)
                .containsExactly(user1, user2);

        assertThat(result.get(0).getTasks()).isEmpty();
        assertThat(result.get(1).getTasks()).hasSize(1);
    }

    @Test
    void getTasksForUsers_emptyUserList_returnsEmpty() {
        var result = internalTaskManagementService.getTasksForUsers(
                List.of(),
                LocalDate.now()
        );

        assertThat(result).isEmpty();

        verifyNoInteractions(taskManagementService);
    }
}
