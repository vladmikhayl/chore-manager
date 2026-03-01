package ru.vladmikhayl.task_management.entity.task;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "task_weekday_assignees")
public class TaskWeekdayAssignee {
    @EmbeddedId
    private TaskWeekdayAssigneeId id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
