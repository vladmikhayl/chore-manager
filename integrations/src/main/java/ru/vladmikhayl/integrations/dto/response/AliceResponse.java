package ru.vladmikhayl.integrations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliceResponse {
    private Response response;
    private String version;

    @Data
    @AllArgsConstructor
    public static class Response {
        private String text;
        private boolean end_session;
    }
}
