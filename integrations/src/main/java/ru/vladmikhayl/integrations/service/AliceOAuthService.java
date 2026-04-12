package ru.vladmikhayl.integrations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.vladmikhayl.integrations.config.AliceOAuthProperties;

@Service
@RequiredArgsConstructor
public class AliceOAuthService {
    private final AliceOAuthProperties properties;

    public void validateAuthorizeRequest(String responseType, String clientId, String redirectUri) {

        if (responseType == null || !responseType.equals("code")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "response_type должен быть code");
        }

        if (clientId == null || !clientId.equals(properties.getClientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный client_id");
        }

        if (redirectUri == null || redirectUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirect_uri обязателен");
        }
    }
}
