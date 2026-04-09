package ru.vladmikhayl.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.vladmikhayl.identity.dto.kafka.TelegramLinkedEvent;
import ru.vladmikhayl.identity.dto.kafka.TelegramUnlinkedEvent;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TELEGRAM_LINKED_TOPIC = "telegram-linked";
    private static final String TELEGRAM_UNLINKED_TOPIC = "telegram-unlinked";

    public void publishTelegramLinked(UUID userId, Long chatId) {
        var event = TelegramLinkedEvent.builder()
                .userId(userId)
                .chatId(chatId)
                .build();

        kafkaTemplate.send(TELEGRAM_LINKED_TOPIC, userId.toString(), event);
    }

    public void publishTelegramUnlinked(UUID userId) {
        var event = TelegramUnlinkedEvent.builder()
                .userId(userId)
                .build();

        kafkaTemplate.send(TELEGRAM_UNLINKED_TOPIC, userId.toString(), event);
    }
}
