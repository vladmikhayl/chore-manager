package ru.vladmikhayl.reminders.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "reminder_user_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderUserSettings {
    @Id
    private UUID userId;

    private Long chatId;

    private boolean dailyReminderEnabled;
}
