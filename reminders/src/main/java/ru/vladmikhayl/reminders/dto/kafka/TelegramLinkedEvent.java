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
public class TelegramLinkedEvent {
    private UUID userId;
    private Long chatId;
}
