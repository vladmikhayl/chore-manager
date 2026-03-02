package ru.vladmikhayl.task_management.entity.task;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "task_completions")
public class TaskCompletion {
    @EmbeddedId
    private TaskCompletionId id;

    @Column(name = "completed_by_user_id", nullable = false)
    private UUID completedByUserId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}
