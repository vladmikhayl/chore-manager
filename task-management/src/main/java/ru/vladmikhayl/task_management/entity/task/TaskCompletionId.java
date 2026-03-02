package ru.vladmikhayl.task_management.entity.task;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class TaskCompletionId implements Serializable {
    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "date", nullable = false)
    private LocalDate date;
}
