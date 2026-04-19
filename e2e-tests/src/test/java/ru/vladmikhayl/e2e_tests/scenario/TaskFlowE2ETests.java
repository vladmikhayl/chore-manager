package ru.vladmikhayl.e2e_tests.scenario;

import org.junit.jupiter.api.Test;
import ru.vladmikhayl.e2e_tests.BaseE2ETest;
import ru.vladmikhayl.e2e_tests.dto.AssignmentType;
import ru.vladmikhayl.e2e_tests.dto.RecurrenceType;
import ru.vladmikhayl.e2e_tests.dto.request.CreateTaskRequest;
import ru.vladmikhayl.e2e_tests.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskFlowE2ETests extends BaseE2ETest {
    @Test
    void testCreateTaskWithFixedAssigneeAndSeeItInTasksForDay() {
        String ownerLogin = generateRandomLogin();
        authHelper.register(ownerLogin, DEFAULT_PASSWORD);
        String ownerToken = authHelper.login(ownerLogin, DEFAULT_PASSWORD).getToken();

        String assigneeLogin = generateRandomLogin();
        authHelper.register(assigneeLogin, DEFAULT_PASSWORD);
        String assigneeToken = authHelper.login(assigneeLogin, DEFAULT_PASSWORD).getToken();

        listHelper.createList(ownerToken, DEFAULT_LIST_TITLE);

        TodoListShortResponse list = listHelper.getLists(ownerToken).stream()
                .filter(it -> it.getTitle().equals(DEFAULT_LIST_TITLE))
                .findFirst()
                .orElseThrow();

        String inviteToken = listHelper.createInvite(ownerToken, list.getId()).getToken();
        listHelper.acceptInvite(assigneeToken, inviteToken);

        TodoListDetailsResponse listDetails = listHelper.getListDetails(ownerToken, list.getId());
        Map<String, UUID> userIdsByLogin = listDetails.getMembers().stream()
                .collect(Collectors.toMap(TodoListMemberResponse::getLogin, TodoListMemberResponse::getUserId));

        UUID assigneeUserId = userIdsByLogin.get(assigneeLogin);

        String taskTitle = generateRandomTaskTitle();

        taskHelper.createTask(
                ownerToken,
                list.getId(),
                CreateTaskRequest.builder()
                        .title(taskTitle)
                        .recurrenceType(RecurrenceType.EveryNdays)
                        .intervalDays(1)
                        .assignmentType(AssignmentType.FixedUser)
                        .fixedUserId(assigneeUserId)
                        .build()
        );

        List<TaskResponse> assigneeTasks = taskHelper.getTasksForDay(assigneeToken, TODAY_DATE_STR);
        TaskResponse assigneeTask = assigneeTasks.stream()
                .filter(task -> task.getTitle().equals(taskTitle))
                .findFirst()
                .orElseThrow();

        assertThat(assigneeTask.getListTitle()).isEqualTo(DEFAULT_LIST_TITLE);
        assertThat(assigneeTask.getAssignmentType()).isEqualTo(AssignmentType.FixedUser);
        assertThat(assigneeTask.getFixedUserId()).isEqualTo(assigneeUserId);
        assertThat(assigneeTask.isCompleted()).isFalse();

        List<TaskResponse> ownerTasks = taskHelper.getTasksForDay(ownerToken, TODAY_DATE_STR);
        assertThat(ownerTasks.stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);

        List<TaskResponse> listTasks = taskHelper.getTasks(ownerToken, list.getId());
        assertThat(listTasks.stream().map(TaskResponse::getTitle))
                .contains(taskTitle);
    }

    @Test
    void testCreateTaskWithWeekdaysRecurrence() {
        String login = generateRandomLogin();
        authHelper.register(login, DEFAULT_PASSWORD);
        String token = authHelper.login(login, DEFAULT_PASSWORD).getToken();

        listHelper.createList(token, DEFAULT_LIST_TITLE);

        TodoListShortResponse list = listHelper.getLists(token).stream()
                .filter(it -> it.getTitle().equals(DEFAULT_LIST_TITLE))
                .findFirst()
                .orElseThrow();

        TodoListDetailsResponse listDetails = listHelper.getListDetails(token, list.getId());
        UUID userId = listDetails.getMembers().get(0).getUserId();

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        int todayWeekday = toApiWeekday(today);
        int tomorrowWeekday = toApiWeekday(tomorrow);

        String taskTitle = generateRandomTaskTitle();

        taskHelper.createTask(
                token,
                list.getId(),
                CreateTaskRequest.builder()
                        .title(taskTitle)
                        .recurrenceType(RecurrenceType.WeeklyByDays)
                        .weekdays(Set.of(todayWeekday))
                        .assignmentType(AssignmentType.FixedUser)
                        .fixedUserId(userId)
                        .build()
        );

        List<TaskResponse> todayTasks = taskHelper.getTasksForDay(token, today.toString());
        assertThat(todayTasks.stream().map(TaskResponse::getTitle))
                .contains(taskTitle);

        List<TaskResponse> tomorrowTasks = taskHelper.getTasksForDay(token, tomorrow.toString());
        assertThat(tomorrowTasks.stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);

        TaskResponse createdTask = todayTasks.stream()
                .filter(task -> task.getTitle().equals(taskTitle))
                .findFirst()
                .orElseThrow();

        assertThat(createdTask.getRecurrenceType()).isEqualTo(RecurrenceType.WeeklyByDays);
        assertThat(createdTask.getWeekdays()).containsExactly(todayWeekday);
        assertThat(createdTask.getWeekdays()).doesNotContain(tomorrowWeekday);
    }

    @Test
    void testRoundRobinAssignmentBetweenParticipants() {
        String ownerLogin = generateRandomLogin();
        authHelper.register(ownerLogin, DEFAULT_PASSWORD);
        String ownerToken = authHelper.login(ownerLogin, DEFAULT_PASSWORD).getToken();

        String user2Login = generateRandomLogin();
        authHelper.register(user2Login, DEFAULT_PASSWORD);
        String user2Token = authHelper.login(user2Login, DEFAULT_PASSWORD).getToken();

        String user3Login = generateRandomLogin();
        authHelper.register(user3Login, DEFAULT_PASSWORD);
        String user3Token = authHelper.login(user3Login, DEFAULT_PASSWORD).getToken();

        listHelper.createList(ownerToken, DEFAULT_LIST_TITLE);

        TodoListShortResponse list = listHelper.getLists(ownerToken).stream()
                .filter(it -> it.getTitle().equals(DEFAULT_LIST_TITLE))
                .findFirst()
                .orElseThrow();

        String inviteToken1 = listHelper.createInvite(ownerToken, list.getId()).getToken();
        listHelper.acceptInvite(user2Token, inviteToken1);

        String inviteToken2 = listHelper.createInvite(ownerToken, list.getId()).getToken();
        listHelper.acceptInvite(user3Token, inviteToken2);

        TodoListDetailsResponse listDetails = listHelper.getListDetails(ownerToken, list.getId());
        Map<String, UUID> userIdsByLogin = listDetails.getMembers().stream()
                .collect(Collectors.toMap(TodoListMemberResponse::getLogin, TodoListMemberResponse::getUserId));

        UUID ownerUserId = userIdsByLogin.get(ownerLogin);
        UUID user2Id = userIdsByLogin.get(user2Login);
        UUID user3Id = userIdsByLogin.get(user3Login);

        String taskTitle = generateRandomTaskTitle();
        LocalDate today = LocalDate.now();

        taskHelper.createTask(
                ownerToken,
                list.getId(),
                CreateTaskRequest.builder()
                        .title(taskTitle)
                        .recurrenceType(RecurrenceType.EveryNdays)
                        .intervalDays(1)
                        .assignmentType(AssignmentType.RoundRobin)
                        .roundRobinUserIds(List.of(ownerUserId, user2Id, user3Id))
                        .build()
        );

        // День 1 -> owner
        assertThat(taskHelper.getTasksForDay(ownerToken, today.toString()).stream().map(TaskResponse::getTitle))
                .contains(taskTitle);
        assertThat(taskHelper.getTasksForDay(user2Token, today.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);
        assertThat(taskHelper.getTasksForDay(user3Token, today.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);

        // День 2 -> user2
        LocalDate day2 = today.plusDays(1);
        assertThat(taskHelper.getTasksForDay(ownerToken, day2.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);
        assertThat(taskHelper.getTasksForDay(user2Token, day2.toString()).stream().map(TaskResponse::getTitle))
                .contains(taskTitle);
        assertThat(taskHelper.getTasksForDay(user3Token, day2.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);

        // День 3 -> user3
        LocalDate day3 = today.plusDays(2);
        assertThat(taskHelper.getTasksForDay(ownerToken, day3.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);
        assertThat(taskHelper.getTasksForDay(user2Token, day3.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);
        assertThat(taskHelper.getTasksForDay(user3Token, day3.toString()).stream().map(TaskResponse::getTitle))
                .contains(taskTitle);

        List<TaskResponse> listTasks = taskHelper.getTasks(ownerToken, list.getId());
        TaskResponse createdTask = listTasks.stream()
                .filter(task -> task.getTitle().equals(taskTitle))
                .findFirst()
                .orElseThrow();

        assertThat(createdTask.getAssignmentType()).isEqualTo(AssignmentType.RoundRobin);
        assertThat(createdTask.getRoundRobinUsers()).extracting(TodoListMemberResponse::getUserId)
                .containsExactly(ownerUserId, user2Id, user3Id);
    }

    @Test
    void testWeekdayAssignmentBetweenParticipants() {
        String ownerLogin = generateRandomLogin();
        authHelper.register(ownerLogin, DEFAULT_PASSWORD);
        String ownerToken = authHelper.login(ownerLogin, DEFAULT_PASSWORD).getToken();

        String user2Login = generateRandomLogin();
        authHelper.register(user2Login, DEFAULT_PASSWORD);
        String user2Token = authHelper.login(user2Login, DEFAULT_PASSWORD).getToken();

        listHelper.createList(ownerToken, DEFAULT_LIST_TITLE);

        TodoListShortResponse list = listHelper.getLists(ownerToken).stream()
                .filter(it -> it.getTitle().equals(DEFAULT_LIST_TITLE))
                .findFirst()
                .orElseThrow();

        String inviteToken = listHelper.createInvite(ownerToken, list.getId()).getToken();
        listHelper.acceptInvite(user2Token, inviteToken);

        TodoListDetailsResponse listDetails = listHelper.getListDetails(ownerToken, list.getId());
        Map<String, UUID> userIdsByLogin = listDetails.getMembers().stream()
                .collect(Collectors.toMap(TodoListMemberResponse::getLogin, TodoListMemberResponse::getUserId));

        UUID ownerUserId = userIdsByLogin.get(ownerLogin);
        UUID user2Id = userIdsByLogin.get(user2Login);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        int todayWeekday = toApiWeekday(today);
        int tomorrowWeekday = toApiWeekday(tomorrow);

        String taskTitle = generateRandomTaskTitle();

        Map<Integer, UUID> weekdayAssignees = createWeekdayMap(ownerUserId);
        weekdayAssignees.put(todayWeekday, ownerUserId);
        weekdayAssignees.put(tomorrowWeekday, user2Id);

        taskHelper.createTask(
                ownerToken,
                list.getId(),
                CreateTaskRequest.builder()
                        .title(taskTitle)
                        .recurrenceType(RecurrenceType.EveryNdays)
                        .intervalDays(1)
                        .assignmentType(AssignmentType.ByWeekday)
                        .weekdayAssignees(weekdayAssignees)
                        .build()
        );

        assertThat(taskHelper.getTasksForDay(ownerToken, today.toString()).stream().map(TaskResponse::getTitle))
                .contains(taskTitle);
        assertThat(taskHelper.getTasksForDay(user2Token, today.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);

        assertThat(taskHelper.getTasksForDay(ownerToken, tomorrow.toString()).stream().map(TaskResponse::getTitle))
                .doesNotContain(taskTitle);
        assertThat(taskHelper.getTasksForDay(user2Token, tomorrow.toString()).stream().map(TaskResponse::getTitle))
                .contains(taskTitle);

        List<TaskResponse> listTasks = taskHelper.getTasks(ownerToken, list.getId());
        TaskResponse createdTask = listTasks.stream()
                .filter(task -> task.getTitle().equals(taskTitle))
                .findFirst()
                .orElseThrow();

        assertThat(createdTask.getAssignmentType()).isEqualTo(AssignmentType.ByWeekday);
        assertThat(createdTask.getWeekdayAssignees().get(todayWeekday)).isEqualTo(ownerUserId);
        assertThat(createdTask.getWeekdayAssignees().get(tomorrowWeekday)).isEqualTo(user2Id);
    }

    @Test
    void testCompleteTaskAndDeleteCompletion() {
        String login = generateRandomLogin();
        authHelper.register(login, DEFAULT_PASSWORD);
        String token = authHelper.login(login, DEFAULT_PASSWORD).getToken();

        listHelper.createList(token, DEFAULT_LIST_TITLE);

        TodoListShortResponse list = listHelper.getLists(token).stream()
                .filter(it -> it.getTitle().equals(DEFAULT_LIST_TITLE))
                .findFirst()
                .orElseThrow();

        TodoListDetailsResponse listDetails = listHelper.getListDetails(token, list.getId());
        UUID userId = listDetails.getMembers().get(0).getUserId();

        String taskTitle = generateRandomTaskTitle();

        UUID taskId = taskHelper.createTask(
                token,
                list.getId(),
                CreateTaskRequest.builder()
                        .title(taskTitle)
                        .recurrenceType(RecurrenceType.EveryNdays)
                        .intervalDays(1)
                        .assignmentType(AssignmentType.FixedUser)
                        .fixedUserId(userId)
                        .build()
        );

        taskHelper.completeTask(token, taskId, TODAY_DATE_STR);

        TaskCompletionStatusResponse completionStatusAfterComplete =
                taskHelper.getTaskCompletion(token, taskId, TODAY_DATE_STR);

        assertThat(completionStatusAfterComplete.isCompleted()).isTrue();
        assertThat(completionStatusAfterComplete.getCompletedByUserId()).isEqualTo(userId);
        assertThat(completionStatusAfterComplete.getCompletedByLogin()).isEqualTo(login);
        assertThat(completionStatusAfterComplete.getCompletedAt()).isNotNull();

        List<TaskResponse> tasksAfterComplete = taskHelper.getTasksForDay(token, TODAY_DATE_STR);
        TaskResponse completedTask = tasksAfterComplete.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElseThrow();

        assertThat(completedTask.isCompleted()).isTrue();

        taskHelper.deleteTaskCompletion(token, taskId, TODAY_DATE_STR);

        TaskCompletionStatusResponse completionStatusAfterDelete =
                taskHelper.getTaskCompletion(token, taskId, TODAY_DATE_STR);

        assertThat(completionStatusAfterDelete.isCompleted()).isFalse();
        assertThat(completionStatusAfterDelete.getCompletedByUserId()).isNull();
        assertThat(completionStatusAfterDelete.getCompletedByLogin()).isNull();
        assertThat(completionStatusAfterDelete.getCompletedAt()).isNull();

        List<TaskResponse> tasksAfterDelete = taskHelper.getTasksForDay(token, TODAY_DATE_STR);
        TaskResponse uncompletedTask = tasksAfterDelete.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElseThrow();

        assertThat(uncompletedTask.isCompleted()).isFalse();
    }
}
