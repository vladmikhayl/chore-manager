package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.task.Task;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    boolean existsByListIdAndTitleIgnoreCase(UUID listId, String title);
}
