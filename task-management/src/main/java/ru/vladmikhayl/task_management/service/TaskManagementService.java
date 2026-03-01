package ru.vladmikhayl.task_management.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.vladmikhayl.task_management.dto.request.CreateTaskRequest;
import ru.vladmikhayl.task_management.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.task_management.dto.response.*;
import ru.vladmikhayl.task_management.entity.*;
import ru.vladmikhayl.task_management.entity.task.*;
import ru.vladmikhayl.task_management.feign.IdentityClient;
import ru.vladmikhayl.task_management.repository.*;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskManagementService {
    private final TodoListRepository todoListRepository;
    private final ListMemberRepository listMemberRepository;
    private final ListInviteRepository listInviteRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentCandidateRepository taskAssignmentCandidateRepository;
    private final TaskWeekdayAssigneeRepository taskWeekdayAssigneeRepository;

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

    @Transactional
    public UUID createTask(UUID userId, UUID listId, CreateTaskRequest request) {
        todoListRepository.findById(listId).orElseThrow(() -> new EntityNotFoundException("Список дел не найден"));

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        String title = request.getTitle().trim();

        if (taskRepository.existsByListIdAndTitleIgnoreCase(listId, title)) {
            throw new DataIntegrityViolationException("В этом списке уже есть задача с таким названием");
        }

        validateCreateTaskRecurrence(request);

        validateCreateTaskAssignment(request, listId);

        Task task = Task.builder()
                .listId(listId)
                .title(title)
                .recurrenceType(request.getRecurrenceType())
                .intervalDays(request.getRecurrenceType() == RecurrenceType.EveryNdays ? request.getIntervalDays() : null)
                .weekdaysMask(request.getRecurrenceType() == RecurrenceType.WeeklyByDays ? WeekdaysMask.toMask(request.getWeekdays()) : null)
                .assignmentType(request.getAssignmentType())
                .fixedUserId(request.getAssignmentType() == AssignmentType.FixedUser ? request.getFixedUserId() : null)
                .rrCursor(request.getAssignmentType() == AssignmentType.RoundRobin ? 0 : null)
                .build();

        task = taskRepository.save(task);

        if (request.getAssignmentType() == AssignmentType.RoundRobin) {
            for (UUID candidateId : request.getRoundRobinUserIds()) {
                taskAssignmentCandidateRepository.save(
                        TaskAssignmentCandidate.builder()
                                .id(new TaskAssignmentCandidateId(task.getId(), candidateId))
                                .build()
                );
            }
        }

        if (request.getAssignmentType() == AssignmentType.ByWeekday) {
            for (var e : request.getWeekdayAssignees().entrySet()) {
                taskWeekdayAssigneeRepository.save(
                        TaskWeekdayAssignee.builder()
                                .id(new TaskWeekdayAssigneeId(task.getId(), e.getKey()))
                                .userId(e.getValue())
                                .build()
                );
            }
        }

        return task.getId();
    }

    @Transactional
    public List<TaskResponse> getTasks(UUID userId, UUID listId) {
        todoListRepository.findById(listId)
                .orElseThrow(() -> new EntityNotFoundException("Список дел не найден"));

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        List<Task> tasks = taskRepository.findAllByListIdOrderByTitleAsc(listId);

        List<TaskResponse> result = new ArrayList<>(tasks.size());

        for (Task task : tasks) {
            TaskResponse.TaskResponseBuilder dto = TaskResponse.builder()
                    .id(task.getId())
                    .listId(task.getListId())
                    .title(task.getTitle())
                    .recurrenceType(task.getRecurrenceType())
                    .intervalDays(task.getIntervalDays())
                    .weekdaysMask(task.getWeekdaysMask())
                    .weekdays(task.getWeekdaysMask() != null ? WeekdaysMask.toSet(task.getWeekdaysMask()) : null)
                    .assignmentType(task.getAssignmentType())
                    .fixedUserId(task.getFixedUserId())
                    .rrCursor(task.getRrCursor());

            if (task.getAssignmentType() == AssignmentType.RoundRobin) {
                List<TaskAssignmentCandidate> candidates =
                        taskAssignmentCandidateRepository.findAllById_TaskId(task.getId());

                List<TodoListMemberResponse> users = new ArrayList<>(candidates.size());

                for (TaskAssignmentCandidate c : candidates) {
                    UUID candidateUserId = c.getId().getUserId();

                    ListMember member = listMemberRepository
                            .findById_ListIdAndId_UserId(listId, candidateUserId)
                            .orElseThrow(() -> new EntityNotFoundException("Кандидат RoundRobin не найден среди участников списка"));

                    users.add(TodoListMemberResponse.builder()
                            .userId(candidateUserId)
                            .login(member.getLogin())
                            .build());
                }

                dto.roundRobinUsers(users);
            }

            if (task.getAssignmentType() == AssignmentType.ByWeekday) {
                List<TaskWeekdayAssignee> assignees =
                        taskWeekdayAssigneeRepository.findAllById_TaskId(task.getId());

                Map<Integer, UUID> map = new LinkedHashMap<>();
                for (TaskWeekdayAssignee a : assignees) {
                    map.put(a.getId().getWeekday(), a.getUserId());
                }

                dto.weekdayAssignees(map);
            }

            result.add(dto.build());
        }

        return result;
    }

    private void validateCreateTaskRecurrence(CreateTaskRequest request) {
        if (request.getRecurrenceType() == RecurrenceType.EveryNdays) {
            if (request.getIntervalDays() == null) {
                throw new IllegalArgumentException("Для EveryNdays нужно указать intervalDays");
            }
        }

        if (request.getRecurrenceType() == RecurrenceType.WeeklyByDays) {
            if (request.getWeekdays() == null || request.getWeekdays().isEmpty()) {
                throw new IllegalArgumentException("Для WeeklyByDays нужно указать weekdays");
            }
        }
    }

    private void validateCreateTaskAssignment(CreateTaskRequest request, UUID listId) {
        if (request.getAssignmentType() == AssignmentType.FixedUser) {
            if (request.getFixedUserId() == null) {
                throw new IllegalArgumentException("Для FixedUser нужно указать fixedUserId");
            }
            assertUserIsMember(listId, request.getFixedUserId());
        }

        if (request.getAssignmentType() == AssignmentType.RoundRobin) {
            var ids = request.getRoundRobinUserIds();
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("Для RoundRobin нужно указать roundRobinUserIds");
            }
            var unique = new LinkedHashSet<>(ids);
            if (unique.size() != ids.size()) {
                throw new IllegalArgumentException("roundRobinUserIds содержит дубликаты");
            }
            for (UUID id : unique) {
                assertUserIsMember(listId, id);
            }
        }

        if (request.getAssignmentType() == AssignmentType.ByWeekday) {
            var map = request.getWeekdayAssignees();
            if (map == null || map.isEmpty()) {
                throw new IllegalArgumentException("Для ByWeekday нужно указать weekdayAssignees");
            }
            if (map.size() != 7) {
                throw new IllegalArgumentException("weekdayAssignees должен содержать все 7 дней");
            }
            for (int d = 0; d <= 6; d++) {
                if (!map.containsKey(d) || map.get(d) == null) {
                    throw new IllegalArgumentException("weekdayAssignees должен содержать ключи 0..6");
                }
                assertUserIsMember(listId, map.get(d));
            }
        }
    }

    private void assertUserIsMember(UUID listId, UUID userId) {
        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new IllegalArgumentException("Выбранный пользователь не состоит в этом списке дел");
        }
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
