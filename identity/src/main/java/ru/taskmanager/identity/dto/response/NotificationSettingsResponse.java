package ru.taskmanager.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class NotificationSettingsResponse {
    private String timezone;
    private boolean dailyReminderEnabled;
    private LocalTime dailyReminderTime;
}
