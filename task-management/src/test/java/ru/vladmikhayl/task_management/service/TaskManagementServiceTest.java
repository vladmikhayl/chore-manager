package ru.vladmikhayl.task_management.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import ru.vladmikhayl.task_management.dto.request.CreateTaskRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.response.CreateInviteResponse;
import ru.vladmikhayl.task_management.dto.response.TodoListShortResponse;
import ru.vladmikhayl.task_management.entity.*;
import ru.vladmikhayl.task_management.entity.task.*;
import ru.vladmikhayl.task_management.feign.IdentityClient;
import ru.vladmikhayl.task_management.repository.*;
import ru.vladmikhayl.task_management.dto.response.TodoListMemberResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskManagementServiceTest {
    private static final UUID LIST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private TodoListRepository todoListRepository;

    @Mock
    private ListMemberRepository listMemberRepository;

    @Mock
    private ListInviteRepository listInviteRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskAssignmentCandidateRepository taskAssignmentCandidateRepository;

    @Mock
    private TaskWeekdayAssigneeRepository taskWeekdayAssigneeRepository;

    @Mock
    private IdentityClient identityClient;

    @Mock
    private Clock clock;

    @InjectMocks
    private TaskManagementService taskManagementService;

    @Test
    void getLists_userHasNoLists_returnsEmptyAndDoesNotQueryLists() {
        UUID userId = UUID.randomUUID();

        when(listMemberRepository.findAllById_UserId(userId))
                .thenReturn(List.of());

        var result = taskManagementService.getLists(userId);

        assertThat(result).isEmpty();

        verify(todoListRepository, never()).findAllByIdIn(any());
        verify(listMemberRepository, never()).countById_ListId(any());
    }

    @Test
    void getLists_success_sortsOwnerFirst_andSortsByTitleWithinEachGroup() {
        UUID userId = UUID.randomUUID();

        UUID ownerId1 = UUID.randomUUID();
        UUID ownerId2 = UUID.randomUUID();
        UUID memberId1 = UUID.randomUUID();
        UUID memberId2 = UUID.randomUUID();

        when(listMemberRepository.findAllById_UserId(userId)).thenReturn(List.of(
                ListMember.builder().id(new ListMemberId(ownerId1, userId)).login("user").build(),
                ListMember.builder().id(new ListMemberId(ownerId2, userId)).login("user").build(),
                ListMember.builder().id(new ListMemberId(memberId1, userId)).login("user").build(),
                ListMember.builder().id(new ListMemberId(memberId2, userId)).login("user").build()
        ));

        // 2 owner-списка
        TodoList ownerZ = TodoList.builder()
                .id(ownerId1)
                .title("z list")
                .ownerUserId(userId)
                .build();

        TodoList ownerA = TodoList.builder()
                .id(ownerId2)
                .title("A list")
                .ownerUserId(userId)
                .build();

        // 2 member-списка
        TodoList memberB = TodoList.builder()
                .id(memberId1)
                .title("b list")
                .ownerUserId(UUID.randomUUID())
                .build();

        TodoList memberAUpper = TodoList.builder()
                .id(memberId2)
                .title("A LIST")
                .ownerUserId(UUID.randomUUID())
                .build();

        when(todoListRepository.findAllByIdIn(
                List.of(ownerId1, ownerId2, memberId1, memberId2)
        )).thenReturn(List.of(ownerZ, memberB, ownerA, memberAUpper));

        when(listMemberRepository.countById_ListId(ownerId1)).thenReturn(1);
        when(listMemberRepository.countById_ListId(ownerId2)).thenReturn(2);
        when(listMemberRepository.countById_ListId(memberId1)).thenReturn(3);
        when(listMemberRepository.countById_ListId(memberId2)).thenReturn(4);

        var result = taskManagementService.getLists(userId);

        assertThat(result).hasSize(4);

        assertThat(result)
                .extracting(
                        TodoListShortResponse::getTitle,
                        TodoListShortResponse::isOwner,
                        TodoListShortResponse::getMembersCount
                )
                .containsExactly(
                        tuple("A list", true, 2),
                        tuple("z list", true, 1),
                        tuple("A LIST", false, 4),
                        tuple("b list", false, 3)
                );
    }

    @Test
    void createList_success_saves() {
        UUID userId = UUID.randomUUID();

        String identityLogin = "vlad_k";
        when(identityClient.getUserLogin(userId)).thenReturn(ResponseEntity.ok(identityLogin));

        CreateTodoListRequest req = CreateTodoListRequest.builder()
                .title("  Домашние дела  ")
                .build();

        when(todoListRepository.existsByOwnerUserIdAndTitle(userId, "Домашние дела"))
                .thenReturn(false);

        UUID savedListId = UUID.randomUUID();

        when(todoListRepository.save(any(TodoList.class)))
                .thenAnswer(inv -> {
                    TodoList arg = inv.getArgument(0);
                    arg.setId(savedListId);
                    return arg;
                });

        taskManagementService.createList(userId, req);

        ArgumentCaptor<TodoList> listCaptor = ArgumentCaptor.forClass(TodoList.class);
        verify(todoListRepository).save(listCaptor.capture());
        TodoList savedList = listCaptor.getValue();
        assertThat(savedList.getTitle()).isEqualTo("Домашние дела");
        assertThat(savedList.getOwnerUserId()).isEqualTo(userId);

        ArgumentCaptor<ListMember> memberCaptor = ArgumentCaptor.forClass(ListMember.class);
        verify(listMemberRepository).save(memberCaptor.capture());
        ListMember savedMember = memberCaptor.getValue();
        assertThat(savedMember.getId().getListId()).isEqualTo(savedListId);
        assertThat(savedMember.getId().getUserId()).isEqualTo(userId);
        assertThat(savedMember.getLogin()).isEqualTo("vlad_k");
    }

    @Test
    void createList_titleAlreadyExistsForOwner_throwsConflict() {
        UUID userId = UUID.randomUUID();

        CreateTodoListRequest req = CreateTodoListRequest.builder()
                .title("Домашние дела")
                .build();

        when(todoListRepository.existsByOwnerUserIdAndTitle(userId, "Домашние дела"))
                .thenReturn(true);

        assertThatThrownBy(() -> taskManagementService.createList(userId, req))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("У вас уже есть список с таким названием");

        verify(todoListRepository, never()).save(any());
        verify(listMemberRepository, never()).save(any());
    }

    @Test
    void createInvite_success_saves() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.existsById(listId)).thenReturn(true);
        when(todoListRepository.existsByIdAndOwnerUserId(listId, userId)).thenReturn(true);

        Instant fixedNow = Instant.parse("2026-02-23T12:00:00Z");
        when(clock.instant()).thenReturn(fixedNow);

        CreateInviteResponse response = taskManagementService.createInvite(userId, listId);

        ArgumentCaptor<ListInvite> inviteCaptor = ArgumentCaptor.forClass(ListInvite.class);
        verify(listInviteRepository).save(inviteCaptor.capture());
        ListInvite saved = inviteCaptor.getValue();

        assertThat(saved.getListId()).isEqualTo(listId);
        assertThat(saved.getCreatedAt()).isEqualTo(fixedNow);
        assertThat(saved.getExpiresAt()).isEqualTo(fixedNow.plus(1, ChronoUnit.DAYS));
        assertThat(saved.getToken()).isNotBlank();

        assertThat(response.getToken()).isEqualTo(saved.getToken());
        assertThat(response.getExpiresAt()).isEqualTo(saved.getExpiresAt());
    }

    @Test
    void createInvite_listNotFound_throwsNotFoundAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.existsById(listId)).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.createInvite(userId, listId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Список дел не найден");

        verify(todoListRepository, never()).existsByIdAndOwnerUserId(any(), any());
        verifyNoInteractions(listInviteRepository);
    }

    @Test
    void createInvite_userNotOwner_throwsForbiddenAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.existsById(listId)).thenReturn(true);
        when(todoListRepository.existsByIdAndOwnerUserId(listId, userId)).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.createInvite(userId, listId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Только создатель списка может создавать приглашения");

        verifyNoInteractions(listInviteRepository);
    }

    @Test
    void acceptInvite_success_saves() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        String token = "TOKEN";

        Instant now = Instant.parse("2026-02-23T12:00:00Z");
        when(clock.instant()).thenReturn(now);

        String identityLogin = "member_login";
        when(identityClient.getUserLogin(userId)).thenReturn(ResponseEntity.ok(identityLogin));

        ListInvite invite = ListInvite.builder()
                .id(UUID.randomUUID())
                .listId(listId)
                .token(token)
                .createdAt(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(3600))
                .build();

        when(listInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(todoListRepository.existsById(listId)).thenReturn(true);
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(false);

        taskManagementService.acceptInvite(userId, token);

        ArgumentCaptor<ListMember> memberCaptor = ArgumentCaptor.forClass(ListMember.class);
        verify(listMemberRepository).save(memberCaptor.capture());
        ListMember saved = memberCaptor.getValue();

        assertThat(saved.getId().getListId()).isEqualTo(listId);
        assertThat(saved.getId().getUserId()).isEqualTo(userId);
        assertThat(saved.getLogin()).isEqualTo("member_login");
    }

    @Test
    void acceptInvite_inviteNotFound_throwsNotFoundAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        String token = "TOKEN";

        when(listInviteRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskManagementService.acceptInvite(userId, token))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Приглашение не найдено");

        verifyNoInteractions(todoListRepository);
        verifyNoInteractions(listMemberRepository);
    }

    @Test
    void acceptInvite_inviteExpired_throwsBadRequestAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        String token = "TOKEN";

        Instant now = Instant.parse("2026-02-23T12:00:00Z");
        when(clock.instant()).thenReturn(now);

        ListInvite invite = ListInvite.builder()
                .id(UUID.randomUUID())
                .listId(listId)
                .token(token)
                .createdAt(now.minusSeconds(3600))
                .expiresAt(now.minusSeconds(1)) // истёк
                .build();

        when(listInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> taskManagementService.acceptInvite(userId, token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Приглашение истекло");

        verifyNoInteractions(todoListRepository);
        verifyNoInteractions(listMemberRepository);
    }

    @Test
    void acceptInvite_listNotFound_throwsNotFoundAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        String token = "TOKEN";

        Instant now = Instant.parse("2026-02-23T12:00:00Z");
        when(clock.instant()).thenReturn(now);

        ListInvite invite = ListInvite.builder()
                .id(UUID.randomUUID())
                .listId(listId)
                .token(token)
                .createdAt(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(3600))
                .build();

        when(listInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(todoListRepository.existsById(listId)).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.acceptInvite(userId, token))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Список дел не найден");

        verifyNoInteractions(listMemberRepository);
    }

    @Test
    void acceptInvite_userAlreadyMember_throwsConflictAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        String token = "TOKEN";

        Instant now = Instant.parse("2026-02-23T12:00:00Z");
        when(clock.instant()).thenReturn(now);

        ListInvite invite = ListInvite.builder()
                .id(UUID.randomUUID())
                .listId(listId)
                .token(token)
                .createdAt(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(3600))
                .build();

        when(listInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(todoListRepository.existsById(listId)).thenReturn(true);
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(true);

        assertThatThrownBy(() -> taskManagementService.acceptInvite(userId, token))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Вы уже состоите в этом списке");

        verify(listMemberRepository, never()).save(any());
    }

    @Test
    void getListDetails_success_userIsOwner() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        TodoList list = TodoList.builder()
                .id(listId)
                .title("Домашние дела")
                .ownerUserId(userId)
                .build();

        when(todoListRepository.findById(listId)).thenReturn(Optional.of(list));
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(true);

        UUID member2 = UUID.randomUUID();

        when(listMemberRepository.findAllById_ListId(listId)).thenReturn(List.of(
                ListMember.builder()
                        .id(new ListMemberId(listId, userId))
                        .login("user1")
                        .build(),
                ListMember.builder()
                        .id(new ListMemberId(listId, member2))
                        .login("user2")
                        .build()
        ));

        var result = taskManagementService.getListDetails(userId, listId);

        assertThat(result.getId()).isEqualTo(listId);
        assertThat(result.getTitle()).isEqualTo("Домашние дела");
        assertThat(result.getOwnerUserId()).isEqualTo(userId);
        assertThat(result.isOwner()).isTrue();

        assertThat(result.getMembers())
                .extracting(TodoListMemberResponse::getUserId, TodoListMemberResponse::getLogin)
                .containsExactly(
                        tuple(userId, "user1"),
                        tuple(member2, "user2")
                );
    }

    @Test
    void getListDetails_success_userIsMemberButNotOwner() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        TodoList list = TodoList.builder()
                .id(listId)
                .title("Список соседей")
                .ownerUserId(ownerId)
                .build();

        when(todoListRepository.findById(listId)).thenReturn(Optional.of(list));
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(true);

        when(listMemberRepository.findAllById_ListId(listId)).thenReturn(List.of(
                ListMember.builder()
                        .id(new ListMemberId(listId, ownerId))
                        .login("owner")
                        .build(),
                ListMember.builder()
                        .id(new ListMemberId(listId, userId))
                        .login("member")
                        .build()
        ));

        var result = taskManagementService.getListDetails(userId, listId);

        assertThat(result.getId()).isEqualTo(listId);
        assertThat(result.getTitle()).isEqualTo("Список соседей");
        assertThat(result.isOwner()).isFalse();
        assertThat(result.getOwnerUserId()).isEqualTo(ownerId);

        assertThat(result.getMembers())
                .extracting(TodoListMemberResponse::getUserId, TodoListMemberResponse::getLogin)
                .containsExactly(
                        tuple(ownerId, "owner"),
                        tuple(userId, "member")
                );
    }

    @Test
    void getListDetails_userNotMember_throwsAccessDenied() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        TodoList list = TodoList.builder()
                .id(listId)
                .title("Домашние дела")
                .ownerUserId(UUID.randomUUID())
                .build();

        when(todoListRepository.findById(listId)).thenReturn(Optional.of(list));
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.getListDetails(userId, listId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Вы не состоите в этом списке дел");
    }

    @Test
    void getListDetails_listNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.findById(listId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskManagementService.getListDetails(userId, listId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Список дел не найден");
    }

    @Test
    void createTask_listNotFound_throwsNotFound() {
        stubListNotFound(LIST_ID);

        var req = baseRequest().build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Список дел не найден");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_userNotMember_throwsForbidden() {
        stubListExists(LIST_ID);
        stubUserIsNotMember(LIST_ID, USER_ID);

        var req = baseRequest().build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Вы не состоите в этом списке дел");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_titleAlreadyExists_throwsConflict_andDoesNotSaveAnything() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);

        String trimmed = "Вынести мусор";
        stubTitleExists(LIST_ID, trimmed);

        var req = baseRequest().build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("В этом списке уже есть задача с таким названием");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_recurrenceEveryNdays_intervalDaysNull_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        var req = baseRequest()
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(null)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для EveryNdays нужно указать intervalDays");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_recurrenceWeeklyByDays_weekdaysNull_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        var req = baseRequest()
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(null)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для WeeklyByDays нужно указать weekdays");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_recurrenceWeeklyByDays_weekdaysEmpty_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        var req = baseRequest()
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(Set.of())
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для WeeklyByDays нужно указать weekdays");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentFixedUser_fixedUserIdNull_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        var req = baseRequest()
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(null)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для FixedUser нужно указать fixedUserId");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentFixedUser_fixedUserNotMember_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID fixed = UUID.randomUUID();
        stubUserIsNotMember(LIST_ID, fixed);

        var req = baseRequest()
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(fixed)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Выбранный пользователь не состоит в этом списке дел");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentRoundRobin_idsNull_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        var req = baseRequest()
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(null)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для RoundRobin нужно указать roundRobinUserIds");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentRoundRobin_hasDuplicates_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID u = UUID.randomUUID();

        var req = baseRequest()
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(List.of(u, u))
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roundRobinUserIds содержит дубликаты");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentRoundRobin_candidateNotMember_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        stubUserIsMember(LIST_ID, c1);
        stubUserIsNotMember(LIST_ID, c2);

        var req = baseRequest()
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(List.of(c1, c2))
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Выбранный пользователь не состоит в этом списке дел");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentByWeekday_mapNull_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        var req = baseRequest()
                .assignmentType(AssignmentType.ByWeekday)
                .weekdayAssignees(null)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для ByWeekday нужно указать weekdayAssignees");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentByWeekday_missingKey_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID u0 = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();

        Map<Integer, UUID> map = allWeekdayAssignees(u0, u1);
        map.remove(3); // дырка

        var req = baseRequest()
                .assignmentType(AssignmentType.ByWeekday)
                .weekdayAssignees(map)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weekdayAssignees должен содержать все 7 дней");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assignmentByWeekday_assigneeNotMember_throwsBadRequest() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID member = UUID.randomUUID();
        UUID notMember = UUID.randomUUID();

        stubUserIsMember(LIST_ID, member);
        stubUserIsNotMember(LIST_ID, notMember);

        // сделаем так, чтобы в одном дне был не-мембер
        Map<Integer, UUID> map = allWeekdayAssignees(member, member);
        map.put(5, notMember);

        var req = baseRequest()
                .assignmentType(AssignmentType.ByWeekday)
                .weekdayAssignees(map)
                .build();

        assertThatThrownBy(() -> taskManagementService.createTask(USER_ID, LIST_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Выбранный пользователь не состоит в этом списке дел");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_success_fixedUser_weeklyByDays() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID fixed = UUID.randomUUID();
        stubUserIsMember(LIST_ID, fixed);

        UUID savedTaskId = UUID.randomUUID();
        stubTaskSavedWithId(savedTaskId);

        var req = CreateTaskRequest.builder()
                .title("  Вынести мусор  ")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(days(0, 2, 4))
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(fixed)
                .build();

        UUID id = taskManagementService.createTask(USER_ID, LIST_ID, req);

        assertThat(id).isEqualTo(savedTaskId);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();

        assertThat(saved.getListId()).isEqualTo(LIST_ID);
        assertThat(saved.getTitle()).isEqualTo("Вынести мусор");
        assertThat(saved.getRecurrenceType()).isEqualTo(RecurrenceType.WeeklyByDays);
        assertThat(saved.getIntervalDays()).isNull();
        assertThat(saved.getWeekdaysMask()).isEqualTo(WeekdaysMask.toMask(Set.of(0, 2, 4)));
        assertThat(saved.getAssignmentType()).isEqualTo(AssignmentType.FixedUser);
        assertThat(saved.getFixedUserId()).isEqualTo(fixed);
        assertThat(saved.getRrCursor()).isNull();

        verifyNoInteractions(taskAssignmentCandidateRepository);
        verifyNoInteractions(taskWeekdayAssigneeRepository);
    }

    @Test
    void createTask_success_roundRobin_everyNdays() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        stubUserIsMember(LIST_ID, c1);
        stubUserIsMember(LIST_ID, c2);

        UUID savedTaskId = UUID.randomUUID();
        stubTaskSavedWithId(savedTaskId);

        var req = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(5)
                .assignmentType(AssignmentType.RoundRobin)
                .roundRobinUserIds(List.of(c1, c2))
                .build();

        UUID id = taskManagementService.createTask(USER_ID, LIST_ID, req);

        assertThat(id).isEqualTo(savedTaskId);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task saved = taskCaptor.getValue();

        assertThat(saved.getListId()).isEqualTo(LIST_ID);
        assertThat(saved.getTitle()).isEqualTo("Вынести мусор");
        assertThat(saved.getRecurrenceType()).isEqualTo(RecurrenceType.EveryNdays);
        assertThat(saved.getIntervalDays()).isEqualTo(5);
        assertThat(saved.getWeekdaysMask()).isNull();
        assertThat(saved.getAssignmentType()).isEqualTo(AssignmentType.RoundRobin);
        assertThat(saved.getRrCursor()).isEqualTo(0);
        assertThat(saved.getFixedUserId()).isNull();

        ArgumentCaptor<TaskAssignmentCandidate> cCaptor = ArgumentCaptor.forClass(TaskAssignmentCandidate.class);
        verify(taskAssignmentCandidateRepository, times(2)).save(cCaptor.capture());

        assertThat(cCaptor.getAllValues())
                .extracting(x -> x.getId().getTaskId(), x -> x.getId().getUserId())
                .containsExactlyInAnyOrder(
                        tuple(savedTaskId, c1),
                        tuple(savedTaskId, c2)
                );

        verifyNoInteractions(taskWeekdayAssigneeRepository);
    }

    @Test
    void createTask_success_byWeekday_weeklyByDays() {
        stubListExists(LIST_ID);
        stubUserIsMember(LIST_ID, USER_ID);
        stubTitleNotExists(LIST_ID, "Вынести мусор");

        UUID u0 = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        stubUserIsMember(LIST_ID, u0);
        stubUserIsMember(LIST_ID, u1);

        UUID savedTaskId = UUID.randomUUID();
        stubTaskSavedWithId(savedTaskId);

        Map<Integer, UUID> map = allWeekdayAssignees(u0, u1);

        var req = CreateTaskRequest.builder()
                .title("Вынести мусор")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .weekdays(days(0, 1, 2, 3, 4, 5))
                .assignmentType(AssignmentType.ByWeekday)
                .weekdayAssignees(map)
                .build();

        UUID id = taskManagementService.createTask(USER_ID, LIST_ID, req);

        assertThat(id).isEqualTo(savedTaskId);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task saved = taskCaptor.getValue();

        assertThat(saved.getListId()).isEqualTo(LIST_ID);
        assertThat(saved.getTitle()).isEqualTo("Вынести мусор");
        assertThat(saved.getRecurrenceType()).isEqualTo(RecurrenceType.WeeklyByDays);
        assertThat(saved.getIntervalDays()).isNull();
        assertThat(saved.getWeekdaysMask()).isEqualTo(WeekdaysMask.toMask(Set.of(0, 1, 2, 3, 4, 5)));
        assertThat(saved.getAssignmentType()).isEqualTo(AssignmentType.ByWeekday);
        assertThat(saved.getRrCursor()).isNull();
        assertThat(saved.getFixedUserId()).isNull();

        verify(taskWeekdayAssigneeRepository, times(7)).save(any(TaskWeekdayAssignee.class));

        ArgumentCaptor<TaskWeekdayAssignee> aCaptor = ArgumentCaptor.forClass(TaskWeekdayAssignee.class);
        verify(taskWeekdayAssigneeRepository, times(7)).save(aCaptor.capture());

        assertThat(aCaptor.getAllValues())
                .extracting(x -> x.getId().getTaskId(), x -> x.getId().getWeekday(), TaskWeekdayAssignee::getUserId)
                .containsExactlyInAnyOrder(
                        tuple(savedTaskId, 0, map.get(0)),
                        tuple(savedTaskId, 1, map.get(1)),
                        tuple(savedTaskId, 2, map.get(2)),
                        tuple(savedTaskId, 3, map.get(3)),
                        tuple(savedTaskId, 4, map.get(4)),
                        tuple(savedTaskId, 5, map.get(5)),
                        tuple(savedTaskId, 6, map.get(6))
                );

        verifyNoInteractions(taskAssignmentCandidateRepository);
    }

    @Test
    void getTasks_listNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.findById(listId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskManagementService.getTasks(userId, listId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Список дел не найден");
    }

    @Test
    void getTasks_userNotMember_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.findById(listId)).thenReturn(Optional.of(TodoList.builder().id(listId).build()));
        stubUserIsNotMember(listId, userId);

        assertThatThrownBy(() -> taskManagementService.getTasks(userId, listId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    var rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("Вы не состоите в этом списке дел");
                });
    }

    @Test
    void getTasks_noTasks_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.findById(listId)).thenReturn(Optional.of(TodoList.builder().id(listId).build()));

        stubUserIsMember(listId, userId);

        when(taskRepository.findAllByListIdOrderByTitleAsc(listId)).thenReturn(List.of());

        var result = taskManagementService.getTasks(userId, listId);

        assertThat(result).isEmpty();
    }

    @Test
    void getTasks_success_mixedAssignments_buildsFullResponses() {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(todoListRepository.findById(listId)).thenReturn(Optional.of(TodoList.builder().id(listId).build()));

        stubUserIsMember(listId, userId);

        // tasks
        UUID fixedTaskId = UUID.randomUUID();
        UUID rrTaskId = UUID.randomUUID();
        UUID byWTaskId = UUID.randomUUID();

        UUID fixedUserId = UUID.randomUUID();

        Task fixedTask = Task.builder()
                .id(fixedTaskId)
                .listId(listId)
                .title("A fixed")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(2)
                .weekdaysMask(null)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(fixedUserId)
                .rrCursor(null)
                .build();

        int maskMonWedFri = WeekdaysMask.toMask(Set.of(0, 2, 4));

        Task rrTask = Task.builder()
                .id(rrTaskId)
                .listId(listId)
                .title("B rr")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .intervalDays(null)
                .weekdaysMask(maskMonWedFri)
                .assignmentType(AssignmentType.RoundRobin)
                .fixedUserId(null)
                .rrCursor(0)
                .build();

        Task byWeekdayTask = Task.builder()
                .id(byWTaskId)
                .listId(listId)
                .title("C byweekday")
                .recurrenceType(RecurrenceType.WeeklyByDays)
                .intervalDays(null)
                .weekdaysMask(maskMonWedFri)
                .assignmentType(AssignmentType.ByWeekday)
                .fixedUserId(null)
                .rrCursor(null)
                .build();

        when(taskRepository.findAllByListIdOrderByTitleAsc(listId)).thenReturn(List.of(fixedTask, rrTask, byWeekdayTask));

        // RoundRobin candidates for rrTask
        UUID cand1 = UUID.randomUUID();
        UUID cand2 = UUID.randomUUID();

        when(taskAssignmentCandidateRepository.findAllById_TaskId(rrTaskId)).thenReturn(List.of(
                TaskAssignmentCandidate.builder().id(new TaskAssignmentCandidateId(rrTaskId, cand1)).build(),
                TaskAssignmentCandidate.builder().id(new TaskAssignmentCandidateId(rrTaskId, cand2)).build()
        ));

        // logins from list_members
        when(listMemberRepository.findById_ListIdAndId_UserId(listId, cand1))
                .thenReturn(Optional.of(ListMember.builder().id(new ListMemberId(listId, cand1)).login("user1").build()));
        when(listMemberRepository.findById_ListIdAndId_UserId(listId, cand2))
                .thenReturn(Optional.of(ListMember.builder().id(new ListMemberId(listId, cand2)).login("user2").build()));

        // ByWeekday assignees for byWTaskId
        UUID mon = UUID.randomUUID();
        UUID tue = UUID.randomUUID();

        when(taskWeekdayAssigneeRepository.findAllById_TaskId(byWTaskId)).thenReturn(List.of(
                TaskWeekdayAssignee.builder()
                        .id(new TaskWeekdayAssigneeId(byWTaskId, 0))
                        .userId(mon)
                        .build(),
                TaskWeekdayAssignee.builder()
                        .id(new TaskWeekdayAssigneeId(byWTaskId, 1))
                        .userId(tue)
                        .build()
        ));

        var result = taskManagementService.getTasks(userId, listId);

        assertThat(result).hasSize(3);

        var fixedDto = result.stream().filter(x -> x.getId().equals(fixedTaskId)).findFirst().orElseThrow();
        assertThat(fixedDto.getTitle()).isEqualTo("A fixed");
        assertThat(fixedDto.getRecurrenceType()).isEqualTo(RecurrenceType.EveryNdays);
        assertThat(fixedDto.getIntervalDays()).isEqualTo(2);
        assertThat(fixedDto.getWeekdaysMask()).isNull();
        assertThat(fixedDto.getWeekdays()).isNull();
        assertThat(fixedDto.getAssignmentType()).isEqualTo(AssignmentType.FixedUser);
        assertThat(fixedDto.getFixedUserId()).isEqualTo(fixedUserId);
        assertThat(fixedDto.getRrCursor()).isNull();
        assertThat(fixedDto.getRoundRobinUsers()).isNull();
        assertThat(fixedDto.getWeekdayAssignees()).isNull();

        var rrDto = result.stream().filter(x -> x.getId().equals(rrTaskId)).findFirst().orElseThrow();
        assertThat(rrDto.getTitle()).isEqualTo("B rr");
        assertThat(rrDto.getRecurrenceType()).isEqualTo(RecurrenceType.WeeklyByDays);
        assertThat(rrDto.getIntervalDays()).isNull();
        assertThat(rrDto.getWeekdaysMask()).isEqualTo(maskMonWedFri);
        assertThat(rrDto.getWeekdays()).containsExactlyInAnyOrder(0, 2, 4);
        assertThat(rrDto.getAssignmentType()).isEqualTo(AssignmentType.RoundRobin);
        assertThat(rrDto.getFixedUserId()).isNull();
        assertThat(rrDto.getRrCursor()).isEqualTo(0);
        assertThat(rrDto.getRoundRobinUsers())
                .extracting(TodoListMemberResponse::getUserId, TodoListMemberResponse::getLogin)
                .containsExactlyInAnyOrder(
                        tuple(cand1, "user1"),
                        tuple(cand2, "user2")
                );
        assertThat(rrDto.getWeekdayAssignees()).isNull();

        var byWDto = result.stream().filter(x -> x.getId().equals(byWTaskId)).findFirst().orElseThrow();
        assertThat(byWDto.getTitle()).isEqualTo("C byweekday");
        assertThat(byWDto.getRecurrenceType()).isEqualTo(RecurrenceType.WeeklyByDays);
        assertThat(byWDto.getIntervalDays()).isNull();
        assertThat(byWDto.getWeekdaysMask()).isEqualTo(maskMonWedFri);
        assertThat(byWDto.getWeekdays()).containsExactlyInAnyOrder(0, 2, 4);
        assertThat(byWDto.getAssignmentType()).isEqualTo(AssignmentType.ByWeekday);
        assertThat(byWDto.getFixedUserId()).isNull();
        assertThat(byWDto.getRrCursor()).isNull();
        assertThat(byWDto.getRoundRobinUsers()).isNull();
        assertThat(byWDto.getWeekdayAssignees())
                .containsEntry(0, mon)
                .containsEntry(1, tue);
    }

    private void stubListExists(UUID listId) {
        when(todoListRepository.findById(listId)).thenReturn(Optional.of(TodoList.builder().id(listId).build()));
    }

    private void stubListNotFound(UUID listId) {
        when(todoListRepository.findById(listId)).thenReturn(Optional.empty());
    }

    private void stubUserIsMember(UUID listId, UUID userId) {
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(true);
    }

    private void stubUserIsNotMember(UUID listId, UUID userId) {
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(false);
    }

    private void stubTitleNotExists(UUID listId, String titleTrimmed) {
        when(taskRepository.existsByListIdAndTitleIgnoreCase(listId, titleTrimmed)).thenReturn(false);
    }

    private void stubTitleExists(UUID listId, String titleTrimmed) {
        when(taskRepository.existsByListIdAndTitleIgnoreCase(listId, titleTrimmed)).thenReturn(true);
    }

    private void stubTaskSavedWithId(UUID savedTaskId) {
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(inv -> {
                    Task t = inv.getArgument(0);
                    t.setId(savedTaskId);
                    return t;
                });
    }

    private CreateTaskRequest.CreateTaskRequestBuilder baseRequest() {
        return CreateTaskRequest.builder()
                .title("  Вынести мусор  ")
                .recurrenceType(RecurrenceType.EveryNdays)
                .intervalDays(3)
                .assignmentType(AssignmentType.FixedUser)
                .fixedUserId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
    }

    private static Map<Integer, UUID> allWeekdayAssignees(UUID u0, UUID u1) {
        Map<Integer, UUID> m = new LinkedHashMap<>();
        for (int d = 0; d <= 6; d++) {
            m.put(d, (d % 2 == 0) ? u0 : u1);
        }
        return m;
    }

    private static Set<Integer> days(Integer... days) {
        return new LinkedHashSet<>(Arrays.asList(days));
    }
}
