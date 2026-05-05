package ru.vladmikhayl.integrations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.vladmikhayl.integrations.config.AliceOAuthProperties;
import ru.vladmikhayl.integrations.dto.response.AliceTokenResponse;
import ru.vladmikhayl.integrations.entity.AliceOAuthAccessToken;
import ru.vladmikhayl.integrations.entity.AliceOAuthAuthorizationCode;
import ru.vladmikhayl.integrations.repository.AliceOAuthAccessTokenRepository;
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
    private final AliceOAuthAccessTokenRepository accessTokenRepository;
    private final AliceOAuthProperties properties;
    private final HashService hashService;
    private final Clock clock;

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

    public String buildRedirectUrl(String redirectUri, String code, String state, String clientId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .queryParam("client_id", clientId);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        return builder.build(true).toUriString();
    }

    public AliceTokenResponse exchangeCodeToAccessToken(
            String grantType,
            String code,
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
        validateTokenRequest(grantType, code, clientId, clientSecret, redirectUri);

        String codeHash = hashService.sha256(code);

        AliceOAuthAuthorizationCode authorizationCode = authorizationCodeRepository.findByCodeHash(codeHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный code"));

        LocalDateTime now = LocalDateTime.now(clock);

        if (authorizationCode.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code уже использован");
        }

        if (authorizationCode.getExpiresAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code истёк");
        }

        authorizationCode.setUsedAt(now);
        authorizationCodeRepository.save(authorizationCode);

        String rawAccessToken = generateRandomToken();
        String tokenHash = hashService.sha256(rawAccessToken);

        AliceOAuthAccessToken accessToken = AliceOAuthAccessToken.builder()
                .tokenHash(tokenHash)
                .userId(authorizationCode.getUserId())
                .expiresAt(now.plusSeconds(properties.getAccessTokenLifetimeSeconds()))
                .build();

        accessTokenRepository.save(accessToken);

        return new AliceTokenResponse(
                rawAccessToken,
                "Bearer",
                properties.getAccessTokenLifetimeSeconds()
        );
    }

    private void validateTokenRequest(
            String grantType,
            String code,
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
        if (grantType == null || !grantType.equals("authorization_code")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grant_type должен быть authorization_code");
        }

        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code обязателен");
        }

        if (clientId == null || !clientId.equals(properties.getClientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный client_id");
        }

        if (clientSecret == null || !clientSecret.equals(properties.getClientSecret())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный client_secret");
        }

        if (redirectUri == null || redirectUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirect_uri обязателен");
        }
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
