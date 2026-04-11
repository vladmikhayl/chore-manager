package ru.vladmikhayl.reminders.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.reminders.entity.ReminderUserSettings;

import java.util.UUID;

public interface ReminderUserSettingsRepository extends JpaRepository<ReminderUserSettings, UUID> {
}
