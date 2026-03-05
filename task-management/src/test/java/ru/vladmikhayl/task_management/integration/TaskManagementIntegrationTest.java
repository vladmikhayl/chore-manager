package ru.vladmikhayl.task_management.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.vladmikhayl.task_management.FeignClientTestConfig;
import ru.vladmikhayl.task_management.dto.request.AcceptInviteRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTaskRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.request.UpdateAssignmentRuleRequest;
import ru.vladmikhayl.task_management.entity.AssignmentType;
import ru.vladmikhayl.task_management.entity.ListInvite;
import ru.vladmikhayl.task_management.entity.RecurrenceType;
import ru.vladmikhayl.task_management.feign.IdentityClient;
import ru.vladmikhayl.task_management.repository.ListInviteRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(FeignClientTestConfig.class)
@TestPropertySource(properties = { "spring.config.location=classpath:/application-test.yml" })
@SpringBootTest
@AutoConfigureMockMvc
public class TaskManagementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ListInviteRepository listInviteRepository;

    @Autowired
    private IdentityClient identityClient;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        TestPostgresContainer postgres = TestPostgresContainer.getInstance();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Test
    void lists_initiallyEmpty_thenCreate_thenGetReturnsOne() throws Exception {
        UUID userId = UUID.randomUUID();

        when(identityClient.getUserLogin(userId)).thenReturn(ResponseEntity.ok("vlad_k"));

        getListsAndExpect200_AndExpectListsSize(userId, 0);

        createListAndExpect201(userId, "Домашние дела");

        getListsAndExpect200(userId)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Домашние дела"))
                .andExpect(jsonPath("$[0].isOwner").value(true))
                .andExpect(jsonPath("$[0].membersCount").value(1));

        String listId = getListsAndExpect200_AndGetFirstListId(userId);

        getListDetailsAndExpect200(userId, listId)
                .andExpect(jsonPath("$.id").value(listId))
                .andExpect(jsonPath("$.title").value("Домашние дела"))
                .andExpect(jsonPath("$.ownerUserId").value(userId.toString()))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.members[0].login").value("vlad_k"));
    }

    @Test
    void createList_duplicateTitle_returns409() throws Exception {
        UUID userId = UUID.randomUUID();

        when(identityClient.getUserLogin(userId)).thenReturn(ResponseEntity.ok("vlad_k"));

        createListAndExpect201(userId, "Домашние дела");

        CreateTodoListRequest req = CreateTodoListRequest.builder()
                .title("Домашние дела")
                .build();

        mockMvc.perform(post("/api/v1/lists")
                        .header("X-User-Id", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("У вас уже есть список с таким названием"))
                .andExpect(jsonPath("$.timestamp").exists());

        getListsAndExpect200(userId)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Домашние дела"));
    }

    @Test
    void inviteFlow_happyPath() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(userA)).thenReturn(ResponseEntity.ok("userA"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB"));

        createListAndExpect201(userA, "Домашние дела");

        String listId = getListsAndExpect200_AndGetFirstListId(userA);

        String token = createInviteAndExpect201(userA, listId);

        getListsAndExpect200_AndExpectListsSize(userB, 0);

        acceptInviteAndExpect200(userB, token);

        getListsAndExpect200(userA)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(listId))
                .andExpect(jsonPath("$[0].isOwner").value(true))
                .andExpect(jsonPath("$[0].membersCount").value(2));

        getListsAndExpect200(userB)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(listId))
                .andExpect(jsonPath("$[0].title").value("Домашние дела"))
                .andExpect(jsonPath("$[0].isOwner").value(false))
                .andExpect(jsonPath("$[0].membersCount").value(2));

        getListDetailsAndExpect200(userA, listId)
                .andExpect(jsonPath("$.id").value(listId))
                .andExpect(jsonPath("$.title").value("Домашние дела"))
                .andExpect(jsonPath("$.ownerUserId").value(userA.toString()))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[?(@.userId=='" + userA + "')].login").value(hasItem("userA")))
                .andExpect(jsonPath("$.members[?(@.userId=='" + userB + "')].login").value(hasItem("userB")));

        getListDetailsAndExpect200(userB, listId)
                .andExpect(jsonPath("$.id").value(listId))
                .andExpect(jsonPath("$.title").value("Домашние дела"))
                .andExpect(jsonPath("$.ownerUserId").value(userA.toString()))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[?(@.userId=='" + userA + "')].login").value(hasItem("userA")))
                .andExpect(jsonPath("$.members[?(@.userId=='" + userB + "')].login").value(hasItem("userB")));
    }

    @Test
    void inviteFlow_userBCreatesOwnList_userADoesNotSeeIt() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(userA)).thenReturn(ResponseEntity.ok("userA"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB"));

        createListAndExpect201(userA, "A");

        String sharedListId = getListsAndExpect200_AndGetFirstListId(userA);

        String token = createInviteAndExpect201(userA, sharedListId);

        acceptInviteAndExpect200(userB, token);

        getListsAndExpect200_AndExpectListsSize(userB, 1);

        createListAndExpect201(userB, "B");

        getListsAndExpect200(userB)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("B"))
                .andExpect(jsonPath("$[1].title").value("A"));

        getListsAndExpect200(userA)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("A"));
    }

    @Test
    void inviteFlow_acceptInviteTwice_returns409() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(userA)).thenReturn(ResponseEntity.ok("userA"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB"));

        createListAndExpect201(userA, "Домашние дела");

        String listId = getListsAndExpect200_AndGetFirstListId(userA);

        String token = createInviteAndExpect201(userA, listId);

        acceptInviteAndExpect200(userB, token);

        acceptInvite(userB, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Вы уже состоите в этом списке"))
                .andExpect(jsonPath("$.timestamp").exists());

        getListsAndExpect200(userB)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].membersCount").value(2));
    }

    @Test
    void acceptInvite_ownerCannotJoinOwnList_returns409() throws Exception {
        UUID owner = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("userA"));

        createListAndExpect201(owner, "A");

        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);

        acceptInvite(owner, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Вы уже состоите в этом списке"))
                .andExpect(jsonPath("$.timestamp").exists());

        getListsAndExpect200(owner)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].membersCount").value(1));
    }

    @Test
    void createInvite_listNotFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID randomListId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/lists/{listId}/invites", randomListId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Список дел не найден"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createInvite_nonOwner_returns403() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("userA"));

        createListAndExpect201(owner, "A");

        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        mockMvc.perform(post("/api/v1/lists/{listId}/invites", listId)
                        .header("X-User-Id", otherUser.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Только создатель списка может создавать приглашения"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void acceptInvite_invalidToken_returns404() throws Exception {
        UUID userId = UUID.randomUUID();

        acceptInvite(userId, "non-existent-token")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Приглашение не найдено"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void acceptInvite_expiredInvite_returns400_andDoesNotAddUser() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("userA"));

        createListAndExpect201(owner, "A");

        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = UUID.randomUUID().toString();

        listInviteRepository.save(
                ListInvite.builder()
                        .listId(UUID.fromString(listId))
                        .token(token)
                        .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                        .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                        .build()
        );

        acceptInvite(user, token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Приглашение истекло"))
                .andExpect(jsonPath("$.timestamp").exists());

        getListsAndExpect200_AndExpectListsSize(user, 0);
    }

    @Test
    void tasksFlow_happyPath_createThenGet() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("owner_login"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB_login"));

        createListAndExpect201(owner, "Домашние дела");
        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);
        acceptInviteAndExpect200(userB, token);

        CreateTaskRequest fixedReq = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(owner)
                .build();

        String fixedTaskId = createTaskAndExpect201_AndGetId(owner, listId, fixedReq);

        CreateTaskRequest rrReq = CreateTaskRequest.builder()
                .title("Покупки")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(Set.of(0, 2, 4))
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(List.of(owner, userB))
                .build();

        String rrTaskId = createTaskAndExpect201_AndGetId(userB, listId, rrReq);

        var tasksJson = getTasksAndExpect200_GetJson(owner, listId);

        assertThat(tasksJson.isArray()).isTrue();
        assertThat(tasksJson.size()).isEqualTo(2);

        // FixedUser task checks
        var fixedNode = findTaskById(tasksJson, fixedTaskId);
        assertThat(fixedNode.get("title").asText()).isEqualTo("Вынести мусор");
        assertThat(fixedNode.get("recurrenceType").asText()).isEqualTo("EveryNdays");
        assertThat(fixedNode.get("intervalDays").asInt()).isEqualTo(3);
        assertThat(fixedNode.get("assignmentType").asText()).isEqualTo("FixedUser");
        assertThat(fixedNode.get("fixedUserId").asText()).isEqualTo(owner.toString());

        // RoundRobin task checks
        var rrNode = findTaskById(tasksJson, rrTaskId);
        assertThat(rrNode.get("title").asText()).isEqualTo("Покупки");
        assertThat(rrNode.get("recurrenceType").asText()).isEqualTo("WeeklyByDays");
        assertThat(rrNode.get("weekdaysMask")).isNotNull();
        assertThat(rrNode.get("weekdays").isArray()).isTrue();
        assertThat(rrNode.get("weekdays")).extracting(JsonNode::asInt).contains(0, 2, 4);
        assertThat(rrNode.get("assignmentType").asText()).isEqualTo("RoundRobin");
        assertThat(rrNode.get("rrCursor").asInt()).isEqualTo(0);
        assertThat(rrNode.get("roundRobinUsers").isArray()).isTrue();
        assertThat(rrNode.get("roundRobinUsers").size()).isEqualTo(2);
        assertThat(rrNode.get("roundRobinUsers").toString())
                .contains(owner.toString()).contains("owner_login")
                .contains(userB.toString()).contains("userB_login");
    }

    @Test
    void updateAssignmentRuleFlow_createTask_thenGetOldRule_thenUpdate_thenGetNewRule() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("owner_login"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB_login"));

        createListAndExpect201(owner, "Домашние дела");
        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);
        acceptInviteAndExpect200(userB, token);

        // task with RoundRobin
        CreateTaskRequest rrReq = CreateTaskRequest.builder()
                .title("Покупки")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(Set.of(0, 2, 4))
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(List.of(owner, userB))
                .build();

        String taskId = createTaskAndExpect201_AndGetId(owner, listId, rrReq);

        var tasksBefore = getTasksAndExpect200_GetJson(owner, listId);
        var nodeBefore = findTaskById(tasksBefore, taskId);

        assertThat(nodeBefore.get("assignmentType").asText()).isEqualTo("RoundRobin");
        assertThat(nodeBefore.get("rrCursor").asInt()).isEqualTo(0);
        assertThat(nodeBefore.get("roundRobinUsers").isArray()).isTrue();
        assertThat(nodeBefore.get("roundRobinUsers").size()).isEqualTo(2);
        assertThat(nodeBefore.get("roundRobinUsers").toString())
                .contains(owner.toString()).contains("owner_login")
                .contains(userB.toString()).contains("userB_login");

        // change to FixedUser
        UpdateAssignmentRuleRequest updateReq = UpdateAssignmentRuleRequest.builder()
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(userB)
                .build();

        mockMvc.perform(put("/api/v1/tasks/{taskId}/assignment-rule", taskId)
                        .header("X-User-Id", owner.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        var tasksAfter = getTasksAndExpect200_GetJson(owner, listId);
        var nodeAfter = findTaskById(tasksAfter, taskId);

        assertThat(nodeAfter.get("assignmentType").asText()).isEqualTo("FixedUser");
        assertThat(nodeAfter.get("fixedUserId").asText()).isEqualTo(userB.toString());
        assertThat(nodeAfter.get("roundRobinUsers").isNull()).isTrue();
        assertThat(nodeAfter.get("rrCursor").isNull()).isTrue();
    }

    @Test
    void deleteTaskFlow() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("owner_login"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB_login"));

        createListAndExpect201(owner, "Домашние дела");
        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);
        acceptInviteAndExpect200(userB, token);

        CreateTaskRequest rrReq = CreateTaskRequest.builder()
                .title("Покупки")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(Set.of(0, 2, 4))
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(List.of(owner, userB))
                .build();

        String rrTaskId = createTaskAndExpect201_AndGetId(owner, listId, rrReq);

        CreateTaskRequest fixedReq = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(owner)
                .build();

        String fixedTaskId = createTaskAndExpect201_AndGetId(userB, listId, fixedReq);

        var tasksBefore = getTasksAndExpect200_GetJson(owner, listId);

        assertThat(tasksBefore.isArray()).isTrue();
        assertThat(tasksBefore.size()).isEqualTo(2);
        assertThat(findTaskById(tasksBefore, rrTaskId).get("title").asText()).isEqualTo("Покупки");
        assertThat(findTaskById(tasksBefore, fixedTaskId).get("title").asText()).isEqualTo("Вынести мусор");

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", rrTaskId)
                        .header("X-User-Id", userB.toString()))
                .andExpect(status().isOk());

        var tasksAfter = getTasksAndExpect200_GetJson(owner, listId);

        assertThat(tasksAfter.isArray()).isTrue();
        assertThat(tasksAfter.size()).isEqualTo(1);

        var remaining = tasksAfter.get(0);
        assertThat(remaining.get("id").asText()).isEqualTo(fixedTaskId);
        assertThat(remaining.get("title").asText()).isEqualTo("Вынести мусор");
    }

    @Test
    void leaveListFlow_userLeavesSuccessfully() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("owner_login"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB_login"));

        createListAndExpect201(owner, "Домашние дела");
        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);
        acceptInviteAndExpect200(userB, token);

        getListsAndExpect200_AndExpectListsSize(userB, 1);

        CreateTaskRequest fixedReq = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(owner)
                .build();

        createTaskAndExpect201_AndGetId(owner, listId, fixedReq);

        mockMvc.perform(delete("/api/v1/lists/{listId}/members/me", listId)
                        .header("X-User-Id", userB.toString()))
                .andExpect(status().isOk());

        getListsAndExpect200_AndExpectListsSize(userB, 0);

        getListsAndExpect200_AndExpectListsSize(owner, 1);
    }

    @Test
    void leaveListFlow_userParticipates_cannotLeave() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("owner_login"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB_login"));

        createListAndExpect201(owner, "Домашние дела");
        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);
        acceptInviteAndExpect200(userB, token);

        getListsAndExpect200_AndExpectListsSize(userB, 1);

        CreateTaskRequest rrReq = CreateTaskRequest.builder()
                .title("Покупки")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(Set.of(0, 2, 4))
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(List.of(owner, userB))
                .build();

        createTaskAndExpect201_AndGetId(owner, listId, rrReq);

        mockMvc.perform(delete("/api/v1/lists/{listId}/members/me", listId)
                        .header("X-User-Id", userB.toString()))
                .andExpect(status().isForbidden());

        getListsAndExpect200_AndExpectListsSize(userB, 1);

        getListsAndExpect200_AndExpectListsSize(owner, 1);
    }

    @Test
    void deleteListFlow_ownerDeletesSuccessfully() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("owner_login"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB_login"));

        createListAndExpect201(owner, "Домашние дела");
        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);
        acceptInviteAndExpect200(userB, token);

        getListsAndExpect200_AndExpectListsSize(owner, 1);
        getListsAndExpect200_AndExpectListsSize(userB, 1);

        mockMvc.perform(delete("/api/v1/lists/{listId}", listId)
                        .header("X-User-Id", owner.toString()))
                .andExpect(status().isOk());

        getListsAndExpect200_AndExpectListsSize(owner, 0);
        getListsAndExpect200_AndExpectListsSize(userB, 0);
    }

    @Test
    void deleteList_notOwner_throwsForbidden() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(identityClient.getUserLogin(owner)).thenReturn(ResponseEntity.ok("owner_login"));
        when(identityClient.getUserLogin(userB)).thenReturn(ResponseEntity.ok("userB_login"));

        createListAndExpect201(owner, "Домашние дела");
        String listId = getListsAndExpect200_AndGetFirstListId(owner);

        String token = createInviteAndExpect201(owner, listId);
        acceptInviteAndExpect200(userB, token);

        getListsAndExpect200_AndExpectListsSize(owner, 1);
        getListsAndExpect200_AndExpectListsSize(userB, 1);

        mockMvc.perform(delete("/api/v1/lists/{listId}", listId)
                        .header("X-User-Id", userB.toString()))
                .andExpect(status().isForbidden());

        getListsAndExpect200_AndExpectListsSize(owner, 1);
        getListsAndExpect200_AndExpectListsSize(userB, 1);
    }

    private ResultActions getListsAndExpect200(UUID userId) throws Exception {
        return mockMvc.perform(get("/api/v1/lists")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());
    }

    private JsonNode getListsAndExpect200_GetJson(UUID userId) throws Exception {
        var mvcResult = getListsAndExpect200(userId)
                .andReturn();

        return objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    }

    private void getListsAndExpect200_AndExpectListsSize(UUID userId, int size) throws Exception {
        getListsAndExpect200(userId)
                .andExpect(jsonPath("$.length()").value(size));
    }

    private void createListAndExpect201(UUID userId, String title) throws Exception {
        CreateTodoListRequest req = CreateTodoListRequest.builder()
                .title(title)
                .build();

        mockMvc.perform(post("/api/v1/lists")
                        .header("X-User-Id", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String createInviteAndExpect201(UUID userId, String listId) throws Exception {
        var mvcResult = mockMvc.perform(post("/api/v1/lists/{listId}/invites", listId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .get("token").asText();
    }

    private void acceptInviteAndExpect200(UUID userId, String token) throws Exception {
        AcceptInviteRequest req = AcceptInviteRequest.builder()
                .token(token)
                .build();

        mockMvc.perform(post("/api/v1/invites/accept")
                        .header("X-User-Id", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private ResultActions acceptInvite(UUID userId, String token) throws Exception {
        AcceptInviteRequest req = AcceptInviteRequest.builder()
                .token(token)
                .build();

        return mockMvc.perform(post("/api/v1/invites/accept")
                .header("X-User-Id", userId.toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private String getListsAndExpect200_AndGetFirstListId(UUID userId) throws Exception {
        return getListsAndExpect200_GetJson(userId).get(0).get("id").asText();
    }

    private ResultActions getListDetailsAndExpect200(UUID userId, String listId) throws Exception {
        return mockMvc.perform(get("/api/v1/lists/{listId}", listId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());
    }

    private String createTaskAndExpect201_AndGetId(UUID userId, String listId, CreateTaskRequest req) throws Exception {
        var mvcResult = mockMvc.perform(post("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(mvcResult.getResponse().getContentAsString()).asText();
    }

    private ResultActions getTasksAndExpect200(UUID userId, String listId) throws Exception {
        return mockMvc.perform(get("/api/v1/lists/{listId}/tasks", listId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());
    }

    private JsonNode getTasksAndExpect200_GetJson(UUID userId, String listId) throws Exception {
        var mvcResult = getTasksAndExpect200(userId, listId).andReturn();
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    }

    private JsonNode findTaskById(JsonNode tasksArray, String taskId) {
        for (JsonNode n : tasksArray) {
            if (n.has("id") && n.get("id").asText().equals(taskId)) {
                return n;
            }
        }
        throw new AssertionError("Task with id=" + taskId + " not found in response: " + tasksArray);
    }
}
