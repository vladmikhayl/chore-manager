package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.task.Task;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    boolean existsByListIdAndTitleIgnoreCase(UUID listId, String title);

    List<Task> findAllByListIdOrderByTitleAsc(UUID listId);
}
