package ru.vladmikhayl.integrations.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramSendMessageRequest {
    private Long chat_id;
    private String text;
}
