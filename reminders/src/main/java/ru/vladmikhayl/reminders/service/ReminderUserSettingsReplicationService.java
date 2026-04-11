package ru.vladmikhayl.reminders.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vladmikhayl.reminders.entity.ReminderUserSettings;
import ru.vladmikhayl.reminders.repository.ReminderUserSettingsRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReminderUserSettingsReplicationService {
    private final ReminderUserSettingsRepository repository;

    @Transactional
    public void handleTelegramLinked(UUID userId, Long chatId) {
        ReminderUserSettings settings = repository.findById(userId)
                .orElseGet(() -> ReminderUserSettings.builder()
                        .userId(userId)
                        .dailyReminderEnabled(false)
                        .build());

        settings.setChatId(chatId);

        repository.save(settings);
    }

    @Transactional
    public void handleTelegramUnlinked(UUID userId) {
        ReminderUserSettings settings = repository.findById(userId)
                .orElseGet(() -> ReminderUserSettings.builder()
                        .userId(userId)
                        .dailyReminderEnabled(false)
                        .build());

        settings.setChatId(null);

        repository.save(settings);
    }

    @Transactional
    public void handleReminderSettingsChanged(UUID userId, boolean dailyReminderEnabled) {
        ReminderUserSettings settings = repository.findById(userId)
                .orElseGet(() -> ReminderUserSettings.builder()
                        .userId(userId)
                        .dailyReminderEnabled(false)
                        .build());

        settings.setDailyReminderEnabled(dailyReminderEnabled);

        repository.save(settings);
    }
}
