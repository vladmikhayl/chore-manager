package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.task.TaskAssignmentCandidate;
import ru.vladmikhayl.task_management.entity.task.TaskAssignmentCandidateId;

import java.util.List;
import java.util.UUID;

public interface TaskAssignmentCandidateRepository extends JpaRepository<TaskAssignmentCandidate, TaskAssignmentCandidateId> {
    List<TaskAssignmentCandidate> findAllById_TaskIdOrderByPositionAsc(UUID taskId);

    void deleteAllById_TaskId(UUID taskId);

    boolean existsById_TaskIdInAndId_UserId(List<UUID> taskIds, UUID userId);
}
