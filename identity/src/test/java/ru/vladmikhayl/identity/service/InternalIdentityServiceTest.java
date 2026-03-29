package ru.vladmikhayl.identity.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import ru.vladmikhayl.identity.dto.request.TelegramLinkRequest;
import ru.vladmikhayl.identity.entity.TelegramLinkToken;
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.entity.UserTelegramAccount;
import ru.vladmikhayl.identity.repository.TelegramLinkTokenRepository;
import ru.vladmikhayl.identity.repository.UserRepository;
import ru.vladmikhayl.identity.repository.UserTelegramAccountRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InternalIdentityServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private TelegramLinkTokenRepository telegramLinkTokenRepository;

    @Mock
    private UserTelegramAccountRepository userTelegramAccountRepository;

    @Mock
    private HashService hashService;

    @Mock
    private Clock clock;

    @InjectMocks
    private InternalIdentityService internalIdentityService;

    @Test
    void getLoginById_userExists_returnsLogin() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .login("user")
                .passwordHash("HASHED")
                .dailyReminderEnabled(false)
                .dailyReminderTime(null)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String result = internalIdentityService.getLoginById(userId);

        assertThat(result).isEqualTo("user");
    }

    @Test
    void getLoginById_userNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalIdentityService.getLoginById(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    void linkTelegramAccount_tokenNotFound_throwsBadCredentials() {
        TelegramLinkRequest request = new TelegramLinkRequest("raw-token", 123456789L);

        when(hashService.sha256("raw-token")).thenReturn("HASHED_TOKEN");
        when(telegramLinkTokenRepository.findByTokenHash("HASHED_TOKEN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalIdentityService.linkTelegramAccount(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Токен привязки недействителен");

        verify(userTelegramAccountRepository, never()).save(any());
        verify(telegramLinkTokenRepository, never()).save(any(TelegramLinkToken.class));
    }

    @Test
    void linkTelegramAccount_tokenAlreadyUsed_throwsBadCredentials() {
        UUID userId = UUID.randomUUID();

        TelegramLinkRequest request = new TelegramLinkRequest("raw-token", 123456789L);

        TelegramLinkToken telegramLinkToken = TelegramLinkToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("HASHED_TOKEN")
                .createdAt(LocalDateTime.of(2026, 3, 29, 12, 0))
                .expiresAt(LocalDateTime.of(2026, 3, 29, 12, 10))
                .usedAt(LocalDateTime.of(2026, 3, 29, 12, 5))
                .build();

        when(hashService.sha256("raw-token")).thenReturn("HASHED_TOKEN");
        when(telegramLinkTokenRepository.findByTokenHash("HASHED_TOKEN"))
                .thenReturn(Optional.of(telegramLinkToken));

        assertThatThrownBy(() -> internalIdentityService.linkTelegramAccount(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Токен привязки уже использован");

        verify(userTelegramAccountRepository, never()).save(any());
        verify(telegramLinkTokenRepository, never()).save(any(TelegramLinkToken.class));
    }

    @Test
    void linkTelegramAccount_tokenExpired_throwsBadCredentials() {
        UUID userId = UUID.randomUUID();

        TelegramLinkRequest request = new TelegramLinkRequest("raw-token", 123456789L);

        TelegramLinkToken telegramLinkToken = TelegramLinkToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("HASHED_TOKEN")
                .createdAt(LocalDateTime.of(2026, 3, 29, 12, 0))
                .expiresAt(LocalDateTime.of(2026, 3, 29, 12, 10))
                .usedAt(null)
                .build();

        when(hashService.sha256("raw-token")).thenReturn("HASHED_TOKEN");
        when(telegramLinkTokenRepository.findByTokenHash("HASHED_TOKEN"))
                .thenReturn(Optional.of(telegramLinkToken));

        when(clock.instant()).thenReturn(Instant.parse("2026-03-29T12:11:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        assertThatThrownBy(() -> internalIdentityService.linkTelegramAccount(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Срок действия токена привязки истёк");

        verify(userTelegramAccountRepository, never()).save(any());
        verify(telegramLinkTokenRepository, never()).save(any(TelegramLinkToken.class));
    }

    @Test
    void linkTelegramAccount_success_savesTelegramAccountAndMarksTokenUsed() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 12, 5);

        TelegramLinkRequest request = new TelegramLinkRequest("raw-token", 123456789L);

        TelegramLinkToken telegramLinkToken = TelegramLinkToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("HASHED_TOKEN")
                .createdAt(LocalDateTime.of(2026, 3, 29, 12, 0))
                .expiresAt(LocalDateTime.of(2026, 3, 29, 12, 10))
                .usedAt(null)
                .build();

        when(hashService.sha256("raw-token")).thenReturn("HASHED_TOKEN");
        when(telegramLinkTokenRepository.findByTokenHash("HASHED_TOKEN"))
                .thenReturn(Optional.of(telegramLinkToken));

        when(clock.instant()).thenReturn(Instant.parse("2026-03-29T12:05:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        internalIdentityService.linkTelegramAccount(request);

        ArgumentCaptor<UserTelegramAccount> accountCaptor =
                ArgumentCaptor.forClass(UserTelegramAccount.class);
        verify(userTelegramAccountRepository).save(accountCaptor.capture());

        UserTelegramAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUserId()).isEqualTo(userId);
        assertThat(savedAccount.getChatId()).isEqualTo(123456789L);

        ArgumentCaptor<TelegramLinkToken> tokenCaptor =
                ArgumentCaptor.forClass(TelegramLinkToken.class);
        verify(telegramLinkTokenRepository).save(tokenCaptor.capture());

        TelegramLinkToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUsedAt()).isEqualTo(now);
    }
}
