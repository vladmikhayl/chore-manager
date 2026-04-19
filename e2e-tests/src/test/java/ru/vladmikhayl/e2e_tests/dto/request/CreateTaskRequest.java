package ru.vladmikhayl.e2e_tests.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vladmikhayl.e2e_tests.dto.AssignmentType;
import ru.vladmikhayl.e2e_tests.dto.RecurrenceType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {
    private String title;
    private RecurrenceType recurrenceType;
    private Integer intervalDays;
    private Set<Integer> weekdays;
    private AssignmentType assignmentType;
    private UUID fixedUserId;
    private List<UUID> roundRobinUserIds;
    private Map<Integer, UUID> weekdayAssignees;
}
