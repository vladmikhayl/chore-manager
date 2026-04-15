package ru.vladmikhayl.integrations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliceConfirmAuthorizeResponse {
    private String redirectUrl;
}
