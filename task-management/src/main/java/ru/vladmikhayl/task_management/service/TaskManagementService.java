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
import ru.vladmikhayl.task_management.dto.request.UpdateAssignmentRuleRequest;
import ru.vladmikhayl.task_management.dto.response.*;
import ru.vladmikhayl.task_management.entity.*;
import ru.vladmikhayl.task_management.entity.task.*;
import ru.vladmikhayl.task_management.feign.IdentityClient;
import ru.vladmikhayl.task_management.repository.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final TaskCompletionRepository taskCompletionRepository;

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

        validateTaskRecurrenceRule(request);

        validateTaskAssignmentRule(
                listId,
                request.getAssignmentType(),
                request.getFixedUserId(),
                request.getRoundRobinUserIds(),
                request.getWeekdayAssignees()
        );

        Task task = Task.builder()
                .startDate(LocalDate.now(clock))
                .listId(listId)
                .title(title)
                .recurrenceType(request.getRecurrenceType())
                .intervalDays(request.getRecurrenceType() == RecurrenceType.EveryNdays ? request.getIntervalDays() : null)
                .weekdaysMask(request.getRecurrenceType() == RecurrenceType.WeeklyByDays ? WeekdaysMask.toMask(request.getWeekdays()) : null)
                .assignmentType(request.getAssignmentType())
                .fixedUserId(request.getAssignmentType() == AssignmentType.FixedUser ? request.getFixedUserId() : null)
                .build();

        task = taskRepository.save(task);

        if (request.getAssignmentType() == AssignmentType.RoundRobin) {
            for (int i = 0; i < request.getRoundRobinUserIds().size(); i++) {
                UUID candidateId = request.getRoundRobinUserIds().get(i);
                taskAssignmentCandidateRepository.save(
                        TaskAssignmentCandidate.builder()
                                .id(new TaskAssignmentCandidateId(task.getId(), candidateId))
                                .position(i)
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

        return tasks.stream()
                .map(this::buildTaskResponse)
                .toList();
    }

    @Transactional
    public void updateAssignmentRule(UUID userId, UUID taskId, UpdateAssignmentRuleRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Задача не найдена"));

        UUID listId = task.getListId();

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        validateTaskAssignmentRule(
                listId,
                request.getAssignmentType(),
                request.getFixedUserId(),
                request.getRoundRobinUserIds(),
                request.getWeekdayAssignees()
        );

        taskAssignmentCandidateRepository.deleteAllById_TaskId(taskId);
        taskWeekdayAssigneeRepository.deleteAllById_TaskId(taskId);

        task.setAssignmentType(request.getAssignmentType());

        if (request.getAssignmentType() == AssignmentType.FixedUser) {
            task.setFixedUserId(request.getFixedUserId());
        }

        if (request.getAssignmentType() == AssignmentType.RoundRobin) {
            task.setFixedUserId(null);

            for (int i = 0; i < request.getRoundRobinUserIds().size(); i++) {
                UUID candidateId = request.getRoundRobinUserIds().get(i);
                taskAssignmentCandidateRepository.save(
                        TaskAssignmentCandidate.builder()
                                .id(new TaskAssignmentCandidateId(taskId, candidateId))
                                .position(i)
                                .build()
                );
            }
        }

        if (request.getAssignmentType() == AssignmentType.ByWeekday) {
            task.setFixedUserId(null);

            for (var e : request.getWeekdayAssignees().entrySet()) {
                taskWeekdayAssigneeRepository.save(
                        TaskWeekdayAssignee.builder()
                                .id(new TaskWeekdayAssigneeId(taskId, e.getKey()))
                                .userId(e.getValue())
                                .build()
                );
            }
        }

        taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(UUID userId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Задача не найдена"));

        UUID listId = task.getListId();

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        taskAssignmentCandidateRepository.deleteAllById_TaskId(taskId);
        taskWeekdayAssigneeRepository.deleteAllById_TaskId(taskId);
        taskCompletionRepository.deleteAllById_TaskId(taskId);

        taskRepository.deleteById(taskId);
    }

    @Transactional
    public void leaveList(UUID userId, UUID listId) {

        TodoList list = todoListRepository.findById(listId)
                .orElseThrow(() -> new EntityNotFoundException("Список не найден"));

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке");
        }

        if (list.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не можете выйти из списка, так как являетесь его создателем");
        }

        List<Task> tasks = taskRepository.findAllByListId(listId);

        List<UUID> taskIds = new ArrayList<>();
        for (Task t : tasks) {
            taskIds.add(t.getId());
        }

        if (!taskIds.isEmpty()) {

            // FixedUser
            boolean usedAsFixedUser =
                    taskRepository.existsByListIdAndAssignmentTypeAndFixedUserId(listId, AssignmentType.FixedUser, userId);

            if (usedAsFixedUser) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Нельзя выйти из списка, участвуя в правилах распределения в задачах списка");
            }

            // RoundRobin
            boolean usedInRR =
                    taskAssignmentCandidateRepository.existsById_TaskIdInAndId_UserId(taskIds, userId);

            if (usedInRR) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Нельзя выйти из списка, участвуя в правилах распределения в задачах списка");
            }

            // WeeklyByDays
            boolean usedInWeekday =
                    taskWeekdayAssigneeRepository.existsById_TaskIdInAndUserId(taskIds, userId);

            if (usedInWeekday) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Нельзя выйти из списка, участвуя в правилах распределения в задачах списка");
            }
        }

        listMemberRepository.deleteById(new ListMemberId(listId, userId));
    }

    @Transactional
    public void deleteList(UUID userId, UUID listId) {

        TodoList list = todoListRepository.findById(listId)
                .orElseThrow(() -> new EntityNotFoundException("Список не найден"));

        if (!list.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Удалить список может только создатель");
        }

        List<Task> tasks = taskRepository.findAllByListId(listId);

        for (Task task : tasks) {
            UUID taskId = task.getId();

            taskAssignmentCandidateRepository.deleteAllById_TaskId(taskId);
            taskWeekdayAssigneeRepository.deleteAllById_TaskId(taskId);
            taskCompletionRepository.deleteAllById_TaskId(taskId);
        }

        taskRepository.deleteAll(tasks);
        listMemberRepository.deleteAllById_ListId(listId);
        todoListRepository.deleteById(listId);
    }

    @Transactional
    public void completeTask(UUID userId, UUID taskId, LocalDate date) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Задача не найдена"));

        UUID listId = task.getListId();

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        TaskCompletionId id = new TaskCompletionId(taskId, date);

        TaskCompletion completion = taskCompletionRepository.findById(id)
                .orElseGet(() -> {
                    TaskCompletion c = new TaskCompletion();
                    c.setId(id);
                    return c;
                });

        completion.setCompletedByUserId(userId);
        completion.setCompletedAt(LocalDateTime.now(clock));

        taskCompletionRepository.save(completion);
    }

    @Transactional
    public TaskCompletionStatusResponse getTaskCompletion(UUID userId, UUID taskId, LocalDate date) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Задача не найдена"));

        UUID listId = task.getListId();

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        TaskCompletionId completionId = new TaskCompletionId(taskId, date);

        Optional<TaskCompletion> completionOptional = taskCompletionRepository.findById(completionId);

        if (completionOptional.isEmpty()) {
            return TaskCompletionStatusResponse.builder()
                    .date(date)
                    .completed(false)
                    .build();
        }

        TaskCompletion completion = completionOptional.get();

        UUID completedByUserId = completion.getCompletedByUserId();

        ListMember member = listMemberRepository.findById_ListIdAndId_UserId(listId, completedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь, отметивший выполнение, не найден среди участников списка"));

        return TaskCompletionStatusResponse.builder()
                .date(date)
                .completed(true)
                .completedByUserId(completedByUserId)
                .completedByLogin(member.getLogin())
                .completedAt(completion.getCompletedAt())
                .build();
    }

    @Transactional
    public void deleteTaskCompletion(UUID userId, UUID taskId, LocalDate date) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Задача не найдена"));

        UUID listId = task.getListId();

        if (!listMemberRepository.existsById_ListIdAndId_UserId(listId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом списке дел");
        }

        taskCompletionRepository.deleteById(new TaskCompletionId(taskId, date));
    }

    @Transactional
    public List<TaskResponse> getTasksForDay(UUID userId, LocalDate date) {
        LocalDate today = LocalDate.now(clock);

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Нельзя выбрать дату в прошлом. Выберите сегодняшнюю дату или более позднюю.");
        }

        List<UUID> listIds = listMemberRepository.findAllById_UserId(userId).stream()
                .map(x -> x.getId().getListId())
                .distinct()
                .toList();

        if (listIds.isEmpty()) {
            return List.of();
        }

        List<Task> tasks = taskRepository.findAllByListIdInOrderByTitleAsc(listIds);

        List<TaskResponse> result = new ArrayList<>();

        for (Task task : tasks) {
            if (!isTaskScheduledOnDate(task, date)) {
                continue;
            }

            UUID assigneeUserId = resolveAssigneeForDate(task, date);

            if (!userId.equals(assigneeUserId)) {
                continue;
            }

            TaskResponse response = buildTaskResponse(task);

            TaskCompletionId completionId = new TaskCompletionId(task.getId(), date);
            Optional<TaskCompletion> completionOptional = taskCompletionRepository.findById(completionId);
            response.setCompleted(completionOptional.isPresent());

            result.add(response);
        }

        return result;
    }

    private boolean isTaskScheduledOnDate(Task task, LocalDate date) {
        if (date.isBefore(task.getStartDate())) {
            return false;
        }

        if (task.getRecurrenceType() == RecurrenceType.EveryNdays) {
            long daysBetween = ChronoUnit.DAYS.between(task.getStartDate(), date);
            return daysBetween % task.getIntervalDays() == 0;
        }

        if (task.getRecurrenceType() == RecurrenceType.WeeklyByDays) {
            int weekday = toWeekdayIndex(date);
            return (task.getWeekdaysMask() & (1 << weekday)) != 0;
        }

        throw new IllegalStateException("Неподдерживаемый recurrenceType: " + task.getRecurrenceType());
    }

    private UUID resolveAssigneeForDate(Task task, LocalDate date) {
        if (task.getAssignmentType() == AssignmentType.FixedUser) {
            return task.getFixedUserId();
        }

        if (task.getAssignmentType() == AssignmentType.ByWeekday) {
            int weekday = toWeekdayIndex(date);

            return taskWeekdayAssigneeRepository.findAllById_TaskId(task.getId()).stream()
                    .filter(x -> x.getId().getWeekday().equals(weekday))
                    .map(TaskWeekdayAssignee::getUserId)
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Исполнитель ByWeekday не найден для дня недели"));
        }

        if (task.getAssignmentType() == AssignmentType.RoundRobin) {
            List<TaskAssignmentCandidate> candidates =
                    taskAssignmentCandidateRepository.findAllById_TaskIdOrderByPositionAsc(task.getId());

            if (candidates.isEmpty()) {
                throw new EntityNotFoundException("Не найдены кандидаты RoundRobin");
            }

            int occurrenceIndex = getOccurrenceIndex(task, date);
            int candidateIndex = occurrenceIndex % candidates.size();

            return candidates.get(candidateIndex).getId().getUserId();
        }

        throw new IllegalStateException("Неподдерживаемый assignmentType: " + task.getAssignmentType());
    }

    private int getOccurrenceIndex(Task task, LocalDate date) {
        if (!isTaskScheduledOnDate(task, date)) {
            throw new IllegalArgumentException("Нельзя вычислить occurrenceIndex для даты, на которую задача не запланирована");
        }

        int occurrenceIndex = 0;

        for (LocalDate d = task.getStartDate(); d.isBefore(date); d = d.plusDays(1)) {
            if (isTaskScheduledOnDate(task, d)) {
                occurrenceIndex++;
            }
        }

        return occurrenceIndex;
    }

    private int toWeekdayIndex(LocalDate date) {
        return date.getDayOfWeek().getValue() - 1;
    }

    private TaskResponse buildTaskResponse(Task task) {
        TodoList list = todoListRepository.findById(task.getListId())
                .orElseThrow(() -> new EntityNotFoundException("Список не найден"));

        TaskResponse.TaskResponseBuilder dto = TaskResponse.builder()
                .id(task.getId())
                .startDate(task.getStartDate())
                .listId(task.getListId())
                .listTitle(list.getTitle())
                .title(task.getTitle())
                .recurrenceType(task.getRecurrenceType())
                .intervalDays(task.getIntervalDays())
                .weekdaysMask(task.getWeekdaysMask())
                .weekdays(task.getWeekdaysMask() != null ? WeekdaysMask.toSet(task.getWeekdaysMask()) : null)
                .assignmentType(task.getAssignmentType())
                .fixedUserId(task.getFixedUserId());

        if (task.getAssignmentType() == AssignmentType.RoundRobin) {
            List<TaskAssignmentCandidate> candidates =
                    taskAssignmentCandidateRepository.findAllById_TaskIdOrderByPositionAsc(task.getId());

            List<TodoListMemberResponse> users = new ArrayList<>(candidates.size());

            for (TaskAssignmentCandidate c : candidates) {
                UUID candidateUserId = c.getId().getUserId();

                ListMember member = listMemberRepository
                        .findById_ListIdAndId_UserId(task.getListId(), candidateUserId)
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

        return dto.build();
    }

    private void validateTaskRecurrenceRule(CreateTaskRequest request) {
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

    private void validateTaskAssignmentRule(UUID listId, AssignmentType assignmentType, UUID fixedUserId, List<UUID> roundRobinUserIds, Map<Integer, UUID> weekdayAssignees) {
        if (assignmentType == AssignmentType.FixedUser) {
            if (fixedUserId == null) {
                throw new IllegalArgumentException("Для FixedUser нужно указать fixedUserId");
            }
            assertUserIsMember(listId, fixedUserId);
        }

        if (assignmentType == AssignmentType.RoundRobin) {
            if (roundRobinUserIds == null || roundRobinUserIds.isEmpty()) {
                throw new IllegalArgumentException("Для RoundRobin нужно указать roundRobinUserIds");
            }
            var unique = new LinkedHashSet<>(roundRobinUserIds);
            if (unique.size() != roundRobinUserIds.size()) {
                throw new IllegalArgumentException("roundRobinUserIds содержит дубликаты");
            }
            for (UUID id : unique) {
                assertUserIsMember(listId, id);
            }
        }

        if (assignmentType == AssignmentType.ByWeekday) {
            if (weekdayAssignees == null || weekdayAssignees.isEmpty()) {
                throw new IllegalArgumentException("Для ByWeekday нужно указать weekdayAssignees");
            }
            if (weekdayAssignees.size() != 7) {
                throw new IllegalArgumentException("weekdayAssignees должен содержать все 7 дней");
            }
            for (int d = 0; d <= 6; d++) {
                if (!weekdayAssignees.containsKey(d) || weekdayAssignees.get(d) == null) {
                    throw new IllegalArgumentException("weekdayAssignees должен содержать ключи 0..6");
                }
                assertUserIsMember(listId, weekdayAssignees.get(d));
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
