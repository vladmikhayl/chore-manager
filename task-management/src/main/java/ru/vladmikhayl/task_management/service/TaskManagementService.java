package ru.vladmikhayl.task_management.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.response.CreateInviteResponse;
import ru.vladmikhayl.task_management.dto.response.TodoListDetailsResponse;
import ru.vladmikhayl.task_management.dto.response.TodoListMemberResponse;
import ru.vladmikhayl.task_management.dto.response.TodoListShortResponse;
import ru.vladmikhayl.task_management.entity.ListInvite;
import ru.vladmikhayl.task_management.entity.ListMember;
import ru.vladmikhayl.task_management.entity.ListMemberId;
import ru.vladmikhayl.task_management.entity.TodoList;
import ru.vladmikhayl.task_management.feign.IdentityClient;
import ru.vladmikhayl.task_management.repository.ListInviteRepository;
import ru.vladmikhayl.task_management.repository.ListMemberRepository;
import ru.vladmikhayl.task_management.repository.TodoListRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskManagementService {
    private final TodoListRepository todoListRepository;
    private final ListMemberRepository listMemberRepository;
    private final ListInviteRepository listInviteRepository;

    private final IdentityClient identityClient;

    private final Clock clock;

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
                        .login(resolveLogin(userId))
                        .build()
        );
    }

    @Transactional
    public CreateInviteResponse createInvite(UUID userId, UUID listId) {

        if (!todoListRepository.existsById(listId)) {
            throw new EntityNotFoundException("Список дел не найден");
        }

        if (!todoListRepository.existsByIdAndOwnerUserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Только создатель списка может создавать приглашения");
        }

        String token = UUID.randomUUID().toString();

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        listInviteRepository.save(
                ListInvite.builder()
                        .listId(listId)
                        .token(token)
                        .createdAt(now)
                        .expiresAt(expiresAt)
                        .build()
        );

        return CreateInviteResponse.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public void acceptInvite(UUID userId, String token) {
        var invite = listInviteRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Приглашение не найдено"));

        Instant now = Instant.now(clock);

        if (invite.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Приглашение истекло");
        }

        UUID listId = invite.getListId();

        if (!todoListRepository.existsById(listId)) {
            throw new EntityNotFoundException("Список дел не найден");
        }

        if (listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new DataIntegrityViolationException("Вы уже состоите в этом списке");
        }

        listMemberRepository.save(
                ListMember.builder()
                        .id(new ListMemberId(listId, userId))
                        .login(resolveLogin(userId))
                        .build()
        );
    }

    @Transactional
    public TodoListDetailsResponse getListDetails(UUID userId, UUID listId) {
        var list = todoListRepository.findById(listId)
                .orElseThrow(() -> new EntityNotFoundException("Список дел не найден"));

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        var members = listMemberRepository.findAllById_ListId(listId).stream()
                .map(m -> TodoListMemberResponse.builder()
                        .userId(m.getId().getUserId())
                        .login(m.getLogin())
                        .build())
                .toList();

        return TodoListDetailsResponse.builder()
                .id(list.getId())
                .title(list.getTitle())
                .ownerUserId(list.getOwnerUserId())
                .isOwner(list.getOwnerUserId().equals(userId))
                .members(members)
                .build();
    }

    private String resolveLogin(UUID userId) {
        try {
            var response = identityClient.getUserLogin(userId);

            String login = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || login == null) {
                throw new IllegalStateException("Не удалось получить логин пользователя");
            }

            return login;
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Пользователь не найден");
        } catch (FeignException e) {
            throw new IllegalStateException("Не удалось получить логин пользователя");
        }
    }
}
