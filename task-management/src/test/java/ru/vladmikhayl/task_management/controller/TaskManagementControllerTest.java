package ru.vladmikhayl.task_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.vladmikhayl.task_management.dto.request.AcceptInviteRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTaskRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.request.UpdateAssignmentRuleRequest;
import ru.vladmikhayl.task_management.entity.AssignmentType;
import ru.vladmikhayl.task_management.entity.RecurrenceType;
import ru.vladmikhayl.task_management.exception.GlobalExceptionHandler;
import ru.vladmikhayl.task_management.service.TaskManagementService;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TaskManagementControllerTest {
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private TaskManagementService taskManagementService;

    @InjectMocks
    private TaskManagementController taskManagementController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(taskManagementController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void createList_success_returns201() throws Exception {
        CreateTodoListRequest req = CreateTodoListRequest.builder()
                .title("Домашние дела")
                .build();

        doNothing().when(taskManagementService).createList(any(UUID.class), any(CreateTodoListRequest.class));

        mockMvc.perform(post("/api/v1/lists")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        verify(taskManagementService).createList(any(UUID.class), any(CreateTodoListRequest.class));
    }

    @Test
    void createList_blankTitle_returns400() throws Exception {
        CreateTodoListRequest req = CreateTodoListRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/v1/lists")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }

    @Test
    void acceptInvite_blankToken_returns400() throws Exception {
        AcceptInviteRequest req = AcceptInviteRequest.builder()
                .token("")
                .build();

        mockMvc.perform(post("/api/v1/invites/accept")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }

    @Test
    void createTask_success_returns201() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID createdTaskId = UUID.randomUUID();

        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(UUID.randomUUID())
                .build();

        when(taskManagementService.createTask(eq(userId), eq(listId), any(CreateTaskRequest.class)))
                .thenReturn(createdTaskId);

        mockMvc.perform(post("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().string("\"" + createdTaskId + "\""));
    }

    @Test
    void createTask_nullTitle_returns400() throws Exception {
        UUID listId = UUID.randomUUID();

        CreateTaskRequest req = CreateTaskRequest.builder()
                .title(null)
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }

    @Test
    void createTask_blankTitle_returns400() throws Exception {
        UUID listId = UUID.randomUUID();

        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }

    @Test
    void createTask_nullRecurrenceType_returns400() throws Exception {
        UUID listId = UUID.randomUUID();

        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(null)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }

    @Test
    void createTask_nullAssignmentType_returns400() throws Exception {
        UUID listId = UUID.randomUUID();

        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(null)
                .fixedUserId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }

    @Test
    void createTask_weekdaysHasOutOfRangeValue_returns400() throws Exception {
        UUID listId = UUID.randomUUID();

        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("Полить цветы")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(Set.of(-1, 2, 4))
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }

    @Test
    void updateAssignmentRule_nullAssignmentType_returns400() throws Exception {
        UUID taskId = UUID.randomUUID();

        UpdateAssignmentRuleRequest req = UpdateAssignmentRuleRequest.builder()
                .assignmentType(null)
                .fixedUserId(UUID.randomUUID())
                .build();

        mockMvc.perform(put("/api/v1/tasks/{taskId}/assignment-rule", taskId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(taskManagementService);
    }
}
