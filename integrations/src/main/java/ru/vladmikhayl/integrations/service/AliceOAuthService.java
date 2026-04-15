package ru.vladmikhayl.integrations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.vladmikhayl.integrations.config.AliceOAuthProperties;
import ru.vladmikhayl.integrations.entity.AliceOAuthAuthorizationCode;
import ru.vladmikhayl.integrations.repository.AliceOAuthAuthorizationCodeRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AliceOAuthService {
    private final AliceOAuthAuthorizationCodeRepository authorizationCodeRepository;
    private final AliceOAuthProperties properties;
    private final HashService hashService;
    private final Clock clock;

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

    public void validateConfirmRequest(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirectUri обязателен");
        }
    }

    public String createAuthorizationCode(UUID userId) {
        String rawCode = generateRandomToken();
        String codeHash = hashService.sha256(rawCode);

        AliceOAuthAuthorizationCode code = AliceOAuthAuthorizationCode.builder()
                .codeHash(codeHash)
                .userId(userId)
                .expiresAt(LocalDateTime.now(clock).plusSeconds(properties.getAuthorizationCodeLifetimeSeconds()))
                .usedAt(null)
                .build();

        authorizationCodeRepository.save(code);

        return rawCode;
    }

    public String buildRedirectUrl(String redirectUri, String code, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        return builder.build(true).toUriString();
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
