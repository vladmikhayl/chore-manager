package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.task.TaskWeekdayAssignee;
import ru.vladmikhayl.task_management.entity.task.TaskWeekdayAssigneeId;

public interface TaskWeekdayAssigneeRepository extends JpaRepository<TaskWeekdayAssignee, TaskWeekdayAssigneeId> {}
