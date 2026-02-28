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
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.entity.ListInvite;
import ru.vladmikhayl.task_management.feign.IdentityClient;
import ru.vladmikhayl.task_management.repository.ListInviteRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
