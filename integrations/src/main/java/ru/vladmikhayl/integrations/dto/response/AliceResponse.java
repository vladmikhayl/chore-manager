package ru.vladmikhayl.integrations.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AliceResponse {
    private Response response;
    private Map<String, Object> start_account_linking;
    private String version;

    @Data
    @AllArgsConstructor
    public static class Response {
        private String text;
        private boolean end_session;
    }
}
