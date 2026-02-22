package ru.vladmikhayl.task_management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.response.TodoListShortResponse;
import ru.vladmikhayl.task_management.entity.ListMember;
import ru.vladmikhayl.task_management.entity.ListMemberId;
import ru.vladmikhayl.task_management.entity.TodoList;
import ru.vladmikhayl.task_management.repository.ListMemberRepository;
import ru.vladmikhayl.task_management.repository.TodoListRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskManagementService {
    private final TodoListRepository todoListRepository;
    private final ListMemberRepository listMemberRepository;

    public List<TodoListShortResponse> getLists(UUID userId) {
        var listIds = listMemberRepository.findAllById_UserId(userId).stream()
                .map(m -> m.getId().getListId())
                .distinct()
                .toList();

        if (listIds.isEmpty()) {
            return List.of();
        }

        var lists = todoListRepository.findAllByIdIn(listIds);

        return lists.stream()
                .map(list -> TodoListShortResponse.builder()
                        .id(list.getId())
                        .title(list.getTitle())
                        .membersCount(listMemberRepository.countById_ListId(list.getId()))
                        .isOwner(list.getOwnerUserId().equals(userId))
                        .build())
                .sorted(
                        Comparator
                                .comparing(TodoListShortResponse::isOwner).reversed()
                                .thenComparing(TodoListShortResponse::getTitle, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();
    }

    @Transactional
    public void createList(UUID userId, CreateTodoListRequest request) {
        var title = request.getTitle().trim();

        if (todoListRepository.existsByOwnerUserIdAndTitle(userId, title)) {
            throw new DataIntegrityViolationException("У вас уже есть список с таким названием");
        }

        var list = TodoList.builder()
                .title(title)
                .ownerUserId(userId)
                .build();

        list = todoListRepository.save(list);

        listMemberRepository.save(
                ListMember.builder()
                        .id(new ListMemberId(list.getId(), userId))
                        .build()
        );
    }
}
