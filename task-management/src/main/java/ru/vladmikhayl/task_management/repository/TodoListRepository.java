package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.TodoList;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TodoListRepository extends JpaRepository<TodoList, UUID> {
    List<TodoList> findAllByIdIn(Collection<UUID> ids);

    boolean existsByOwnerUserIdAndTitle(UUID ownerUserId, String title);
}
