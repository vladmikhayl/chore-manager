package ru.vladmikhayl.integrations.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliceConfirmAuthorizeRequest {
    private String redirectUri;
    private String state;
}
