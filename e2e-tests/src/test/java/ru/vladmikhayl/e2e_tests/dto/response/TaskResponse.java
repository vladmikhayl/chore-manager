package ru.vladmikhayl.e2e_tests.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vladmikhayl.e2e_tests.dto.AssignmentType;
import ru.vladmikhayl.e2e_tests.dto.RecurrenceType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private UUID id;
    private LocalDate startDate;
    private UUID listId;
    private String listTitle;
    private String title;
    private RecurrenceType recurrenceType;
    private Integer intervalDays;
    private Integer weekdaysMask;
    private Set<Integer> weekdays;
    private AssignmentType assignmentType;
    private UUID fixedUserId;
    private List<TodoListMemberResponse> roundRobinUsers;
    private Map<Integer, UUID> weekdayAssignees;
    private boolean completed;
}
