package ru.vladmikhayl.task_management.entity.task;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class TaskWeekdayAssigneeId implements Serializable {
    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "weekday", nullable = false)
    private Integer weekday;
}
