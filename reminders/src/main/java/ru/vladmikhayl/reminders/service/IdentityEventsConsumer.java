package ru.vladmikhayl.reminders.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.vladmikhayl.reminders.dto.kafka.ReminderSettingsChangedEvent;
import ru.vladmikhayl.reminders.dto.kafka.TelegramLinkedEvent;
import ru.vladmikhayl.reminders.dto.kafka.TelegramUnlinkedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventsConsumer {
    private final ReminderUserSettingsReplicationService replicationService;

    @KafkaListener(
            topics = "telegram-linked",
            containerFactory = "telegramLinkedKafkaListenerContainerFactory"
    )
    public void handleTelegramLinked(TelegramLinkedEvent event) {
        log.info("Received telegram-linked event for userId={}, chatId={}",
                event.getUserId(), event.getChatId());

        replicationService.handleTelegramLinked(event.getUserId(), event.getChatId());
    }

    @KafkaListener(
            topics = "telegram-unlinked",
            containerFactory = "telegramUnlinkedKafkaListenerContainerFactory"
    )
    public void handleTelegramUnlinked(TelegramUnlinkedEvent event) {
        log.info("Received telegram-unlinked event for userId={}", event.getUserId());

        replicationService.handleTelegramUnlinked(event.getUserId());
    }

    @KafkaListener(
            topics = "reminder-settings-changed",
            containerFactory = "reminderSettingsChangedKafkaListenerContainerFactory"
    )
    public void handleReminderSettingsChanged(ReminderSettingsChangedEvent event) {
        log.info("Received reminder-settings-changed event for userId={}, enabled={}",
                event.getUserId(), event.getDailyReminderEnabled());

        replicationService.handleReminderSettingsChanged(
                event.getUserId(),
                Boolean.TRUE.equals(event.getDailyReminderEnabled())
        );
    }
}
