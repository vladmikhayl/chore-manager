package ru.vladmikhayl.task_management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.response.TodoListShortResponse;
import ru.vladmikhayl.task_management.entity.ListMember;
import ru.vladmikhayl.task_management.entity.ListMemberId;
import ru.vladmikhayl.task_management.entity.TodoList;
import ru.vladmikhayl.task_management.repository.ListMemberRepository;
import ru.vladmikhayl.task_management.repository.TodoListRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskManagementServiceTest {
    @Mock
    private TodoListRepository todoListRepository;

    @Mock
    private ListMemberRepository listMemberRepository;

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
}
