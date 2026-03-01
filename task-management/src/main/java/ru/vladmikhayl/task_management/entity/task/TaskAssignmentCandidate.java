package ru.vladmikhayl.task_management.entity.task;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "task_assignment_candidates")
public class TaskAssignmentCandidate {
    @EmbeddedId
    private TaskAssignmentCandidateId id;
}
