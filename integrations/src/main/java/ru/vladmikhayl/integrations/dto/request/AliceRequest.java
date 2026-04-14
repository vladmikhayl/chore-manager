package ru.vladmikhayl.integrations.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Webhook-запрос от Алисы")
public class AliceRequest {
    private Request request;
    private Session session;
    private String version;

    @Data
    public static class Request {
        private String command;
        private String original_utterance;
    }

    @Data
    public static class Session {
        private String session_id;
        private String message_id;
        private String user_id;
    }
}
