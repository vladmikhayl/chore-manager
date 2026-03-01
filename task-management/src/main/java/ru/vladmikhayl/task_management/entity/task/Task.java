package ru.vladmikhayl.task_management.entity.task;

import jakarta.persistence.*;
import lombok.*;
import ru.vladmikhayl.task_management.entity.AssignmentType;
import ru.vladmikhayl.task_management.entity.RecurrenceType;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "list_id", nullable = false)
    private UUID listId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false)
    private RecurrenceType recurrenceType;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "weekdays_mask")
    private Integer weekdaysMask;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false)
    private AssignmentType assignmentType;

    @Column(name = "fixed_user_id")
    private UUID fixedUserId;

    @Column(name = "rr_cursor")
    private Integer rrCursor;
}
