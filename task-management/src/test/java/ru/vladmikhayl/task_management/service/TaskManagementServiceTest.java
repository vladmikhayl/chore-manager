package ru.vladmikhayl.task_management.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.response.CreateInviteResponse;
import ru.vladmikhayl.task_management.dto.response.TodoListShortResponse;
import ru.vladmikhayl.task_management.entity.ListInvite;
import ru.vladmikhayl.task_management.entity.ListMember;
import ru.vladmikhayl.task_management.entity.ListMemberId;
import ru.vladmikhayl.task_management.entity.TodoList;
import ru.vladmikhayl.task_management.repository.ListInviteRepository;
import ru.vladmikhayl.task_management.repository.ListMemberRepository;
import ru.vladmikhayl.task_management.repository.TodoListRepository;

import java.nio.file.AccessDeniedException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskManagementServiceTest {
    @Mock
    private TodoListRepository todoListRepository;

    @Mock
    private ListMemberRepository listMemberRepository;

    @Mock
    private ListInviteRepository listInviteRepository;

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
                ListMember.builder().id(new ListMemberId(ownerId1, userId)).build(),
                ListMember.builder().id(new ListMemberId(ownerId2, userId)).build(),
                ListMember.builder().id(new ListMemberId(memberId1, userId)).build(),
                ListMember.builder().id(new ListMemberId(memberId2, userId)).build()
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
    void createInvite_success_saves() throws AccessDeniedException {
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
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Только создатель списка может создавать приглашения");

        verifyNoInteractions(listInviteRepository);
    }

    @Test
    void acceptInvite_success_saves() {
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
        when(listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)).thenReturn(false);

        taskManagementService.acceptInvite(userId, token);

        ArgumentCaptor<ListMember> memberCaptor = ArgumentCaptor.forClass(ListMember.class);
        verify(listMemberRepository).save(memberCaptor.capture());
        ListMember saved = memberCaptor.getValue();

        assertThat(saved.getId().getListId()).isEqualTo(listId);
        assertThat(saved.getId().getUserId()).isEqualTo(userId);
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
}
