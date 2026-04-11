package ru.vladmikhayl.reminders.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderSettingsChangedEvent {
    private UUID userId;
    private Boolean dailyReminderEnabled;
}
