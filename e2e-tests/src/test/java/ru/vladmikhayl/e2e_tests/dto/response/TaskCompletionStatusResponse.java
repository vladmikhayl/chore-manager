package ru.vladmikhayl.e2e_tests.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCompletionStatusResponse {
    private LocalDate date;
    private boolean completed;
    private UUID completedByUserId;
    private String completedByLogin;
    private LocalDateTime completedAt;
}
