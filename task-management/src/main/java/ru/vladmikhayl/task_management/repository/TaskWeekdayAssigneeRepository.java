package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.task.TaskWeekdayAssignee;
import ru.vladmikhayl.task_management.entity.task.TaskWeekdayAssigneeId;

import java.util.List;
import java.util.UUID;

public interface TaskWeekdayAssigneeRepository extends JpaRepository<TaskWeekdayAssignee, TaskWeekdayAssigneeId> {
    List<TaskWeekdayAssignee> findAllById_TaskId(UUID taskId);

    void deleteAllById_TaskId(UUID taskId);

    boolean existsById_TaskIdInAndUserId(List<UUID> taskIds, UUID userId);
}
