package ru.vladmikhayl.integrations.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.vladmikhayl.integrations.config.AliceOAuthProperties;
import ru.vladmikhayl.integrations.dto.response.AliceTokenResponse;
import ru.vladmikhayl.integrations.entity.AliceOAuthAccessToken;
import ru.vladmikhayl.integrations.entity.AliceOAuthAuthorizationCode;
import ru.vladmikhayl.integrations.repository.AliceOAuthAccessTokenRepository;
import ru.vladmikhayl.integrations.repository.AliceOAuthAuthorizationCodeRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AliceOAuthServiceTest {
    private static final UUID USER_ID = UUID.fromString("8f0118cc-029b-4d69-a39a-44e2ae956f8d");
    private static final String RAW_CODE = "test-code";
    private static final String CODE_HASH = "hashed-code";
    private static final String CLIENT_ID = "chore-manager-alice";
    private static final String CLIENT_SECRET = "super-secret";
    private static final String REDIRECT_URI = "https://dialogs.yandex.ru/redirect";

    @Mock
    private AliceOAuthAuthorizationCodeRepository authorizationCodeRepository;

    @Mock
    private AliceOAuthAccessTokenRepository accessTokenRepository;

    @Mock
    private HashService hashService;

    private AliceOAuthProperties properties;
    private Clock clock;

    private AliceOAuthService aliceOAuthService;

    @BeforeEach
    void setUp() {
        properties = new AliceOAuthProperties();
        properties.setClientId(CLIENT_ID);
        properties.setClientSecret(CLIENT_SECRET);
        properties.setAuthorizationCodeLifetimeSeconds(60);
        properties.setAccessTokenLifetimeSeconds(3600);

        clock = Clock.fixed(Instant.parse("2026-04-15T12:00:00Z"), ZoneOffset.UTC);

        aliceOAuthService = new AliceOAuthService(
                authorizationCodeRepository,
                accessTokenRepository,
                properties,
                hashService,
                clock
        );
    }

    @Test
    void exchangeCodeToAccessToken_validCode_returnsTokenAndMarksCodeAsUsed() {
        AliceOAuthAuthorizationCode authorizationCode = AliceOAuthAuthorizationCode.builder()
                .id(UUID.randomUUID())
                .codeHash(CODE_HASH)
                .userId(USER_ID)
                .expiresAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusMinutes(1))
                .usedAt(null)
                .build();

        when(hashService.sha256(anyString())).thenReturn("hashed-access-token");
        when(hashService.sha256(RAW_CODE)).thenReturn(CODE_HASH);
        when(authorizationCodeRepository.findByCodeHash(CODE_HASH))
                .thenReturn(Optional.of(authorizationCode));

        AliceTokenResponse response = aliceOAuthService.exchangeCodeToAccessToken(
                "authorization_code",
                RAW_CODE,
                CLIENT_ID,
                CLIENT_SECRET,
                REDIRECT_URI
        );

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600);

        assertThat(authorizationCode.getUsedAt())
                .isEqualTo(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        verify(authorizationCodeRepository).save(authorizationCode);

        ArgumentCaptor<AliceOAuthAccessToken> accessTokenCaptor =
                ArgumentCaptor.forClass(AliceOAuthAccessToken.class);
        verify(accessTokenRepository).save(accessTokenCaptor.capture());

        AliceOAuthAccessToken savedToken = accessTokenCaptor.getValue();
        assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
        assertThat(savedToken.getTokenHash()).isNotBlank();
        assertThat(savedToken.getExpiresAt())
                .isEqualTo(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusSeconds(3600));
    }

    @Test
    void exchangeCodeToAccessToken_codeNotFound_throwsBadRequest() {
        when(hashService.sha256(RAW_CODE)).thenReturn(CODE_HASH);
        when(authorizationCodeRepository.findByCodeHash(CODE_HASH))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> aliceOAuthService.exchangeCodeToAccessToken(
                        "authorization_code",
                        RAW_CODE,
                        CLIENT_ID,
                        CLIENT_SECRET,
                        REDIRECT_URI
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).isEqualTo("Некорректный code");

        verify(authorizationCodeRepository, never()).save(any());
        verify(accessTokenRepository, never()).save(any());
    }

    @Test
    void exchangeCodeToAccessToken_codeAlreadyUsed_throwsBadRequest() {
        AliceOAuthAuthorizationCode authorizationCode = AliceOAuthAuthorizationCode.builder()
                .id(UUID.randomUUID())
                .codeHash(CODE_HASH)
                .userId(USER_ID)
                .expiresAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusMinutes(1))
                .usedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).minusSeconds(10))
                .build();

        when(hashService.sha256(RAW_CODE)).thenReturn(CODE_HASH);
        when(authorizationCodeRepository.findByCodeHash(CODE_HASH))
                .thenReturn(Optional.of(authorizationCode));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> aliceOAuthService.exchangeCodeToAccessToken(
                        "authorization_code",
                        RAW_CODE,
                        CLIENT_ID,
                        CLIENT_SECRET,
                        REDIRECT_URI
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).isEqualTo("Code уже использован");

        verify(authorizationCodeRepository, never()).save(any());
        verify(accessTokenRepository, never()).save(any());
    }

    @Test
    void exchangeCodeToAccessToken_codeExpired_throwsBadRequest() {
        AliceOAuthAuthorizationCode authorizationCode = AliceOAuthAuthorizationCode.builder()
                .id(UUID.randomUUID())
                .codeHash(CODE_HASH)
                .userId(USER_ID)
                .expiresAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).minusSeconds(1))
                .usedAt(null)
                .build();

        when(hashService.sha256(RAW_CODE)).thenReturn(CODE_HASH);
        when(authorizationCodeRepository.findByCodeHash(CODE_HASH))
                .thenReturn(Optional.of(authorizationCode));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> aliceOAuthService.exchangeCodeToAccessToken(
                        "authorization_code",
                        RAW_CODE,
                        CLIENT_ID,
                        CLIENT_SECRET,
                        REDIRECT_URI
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).isEqualTo("Code истёк");

        verify(authorizationCodeRepository, never()).save(any());
        verify(accessTokenRepository, never()).save(any());
    }

    @Test
    void exchangeCodeToAccessToken_invalidClientSecret_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> aliceOAuthService.exchangeCodeToAccessToken(
                        "authorization_code",
                        RAW_CODE,
                        CLIENT_ID,
                        "wrong-secret",
                        REDIRECT_URI
                )
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).isEqualTo("Некорректный client_secret");

        verifyNoInteractions(authorizationCodeRepository, accessTokenRepository, hashService);
    }
}
