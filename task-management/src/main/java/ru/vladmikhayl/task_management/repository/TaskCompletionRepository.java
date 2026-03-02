package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.task.TaskCompletion;
import ru.vladmikhayl.task_management.entity.task.TaskCompletionId;

import java.util.UUID;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, TaskCompletionId> {
    void deleteAllById_TaskId(UUID taskId);
}
