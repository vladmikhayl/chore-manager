package ru.vladmikhayl.task_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderTaskResponse {
    private UUID taskId;
    private String title;
    private String listTitle;
    private boolean completed;
}
