package ru.vladmikhayl.identity.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.vladmikhayl.identity.dto.request.LoginRequest;
import ru.vladmikhayl.identity.dto.request.NotificationSettingsRequest;
import ru.vladmikhayl.identity.dto.request.RegisterRequest;
import ru.vladmikhayl.identity.dto.response.LoginResponse;
import ru.vladmikhayl.identity.dto.response.ProfileResponse;
import ru.vladmikhayl.identity.dto.response.TelegramLinkResponse;
import ru.vladmikhayl.identity.entity.TelegramLinkToken;
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.entity.UserTelegramAccount;
import ru.vladmikhayl.identity.repository.TelegramLinkTokenRepository;
import ru.vladmikhayl.identity.repository.UserRepository;
import ru.vladmikhayl.identity.repository.UserTelegramAccountRepository;
import ru.vladmikhayl.identity.security.JwtService;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IdentityServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private HashService hashService;

    @InjectMocks
    private IdentityService identityService;

    @Mock
    private TelegramLinkTokenRepository telegramLinkTokenRepository;

    @Mock
    private UserTelegramAccountRepository userTelegramAccountRepository;

    @Mock
    private Clock clock;

    @Test
    void register_success_savesUserWithDefaults() {
        RegisterRequest req = RegisterRequest.builder()
                .login("username")
                .password("password")
                .build();

        when(userRepository.existsByLogin("username")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("HASHED");

        identityService.register(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getLogin()).isEqualTo("username");
        assertThat(saved.getPasswordHash()).isEqualTo("HASHED");
        assertThat(saved.isDailyReminderEnabled()).isFalse();
        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void register_loginAlreadyTaken_throwsConflictAndDoesNotSave() {
        RegisterRequest req = RegisterRequest.builder()
                .login("username")
                .password("password")
                .build();

        when(userRepository.existsByLogin("username")).thenReturn(true);

        assertThatThrownBy(() -> identityService.register(req))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Этот логин уже занят");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = LoginRequest.builder()
                .login("username")
                .password("password")
                .build();

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .login("username")
                .passwordHash("HASHED")
                .build();

        when(userRepository.findByLogin("username"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "HASHED"))
                .thenReturn(true);
        when(jwtService.generateToken(userId, "username"))
                .thenReturn("JWT_TOKEN");

        LoginResponse response = identityService.login(request);

        assertThat(response.getToken()).isEqualTo("JWT_TOKEN");
    }

    @Test
    void login_userNotFound_throwsBadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .login("username")
                .password("password")
                .build();

        when(userRepository.findByLogin("username"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Неверный логин или пароль");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .login("username")
                .password("password")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .login("username")
                .passwordHash("HASHED")
                .build();

        when(userRepository.findByLogin("username"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "HASHED"))
                .thenReturn(false);

        assertThatThrownBy(() -> identityService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Неверный логин или пароль");
    }

    @Test
    void getProfile_success_returnsSettings() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .login("vlad")
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(9, 0))
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        ProfileResponse response = identityService.getProfile(userId);

        assertThat(response.getLogin()).isEqualTo("vlad");
        assertThat(response.isDailyReminderEnabled()).isTrue();
    }

    @Test
    void getProfile_userNotFound_throwsEntityNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityService.getProfile(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    void updateNotificationSettings_userNotFound_throwsEntityNotFound() {
        UUID userId = UUID.randomUUID();

        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .dailyReminderEnabled(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityService.updateNotificationSettings(userId, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateNotificationSettings_onlyTime_updatesTimeAndDoesNotTouchOthers() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(8, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .dailyReminderTime(LocalTime.of(10, 0))
                .build();

        identityService.updateNotificationSettings(userId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(saved.isDailyReminderEnabled()).isTrue();
    }

    @Test
    void updateNotificationSettings_onlyEnabled_updatesEnabledAndDoesNotTouchOthers() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .dailyReminderEnabled(false)
                .dailyReminderTime(LocalTime.of(15, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .dailyReminderEnabled(true)
                .build();

        identityService.updateNotificationSettings(userId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(saved.isDailyReminderEnabled()).isTrue();
    }

    @Test
    void updateNotificationSettings_allFields_updatesAll() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .dailyReminderEnabled(false)
                .dailyReminderTime(LocalTime.of(8, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(7, 0))
                .build();

        identityService.updateNotificationSettings(userId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.isDailyReminderEnabled()).isTrue();
        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    void updateNotificationSettings_emptyRequest_doesNotChangeAnything() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(8, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingsRequest req = NotificationSettingsRequest.builder().build();

        identityService.updateNotificationSettings(userId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.isDailyReminderEnabled()).isTrue();
        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void createTelegramLinkToken_userNotFound_throwsEntityNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> identityService.createTelegramLinkToken(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");

        verify(telegramLinkTokenRepository, never()).save(any());
    }

    @Test
    void createTelegramLinkToken_success_savesTokenAndReturnsRawToken() {
        UUID userId = UUID.randomUUID();
        Instant nowInstant = Instant.parse("2026-03-29T12:00:00Z");
        ZoneId zoneId = ZoneId.of("Europe/Moscow");

        when(userRepository.existsById(userId)).thenReturn(true);
        when(hashService.sha256(any())).thenReturn("HASHED");
        when(clock.instant()).thenReturn(nowInstant);
        when(clock.getZone()).thenReturn(zoneId);

        String rawToken = identityService.createTelegramLinkToken(userId);

        ArgumentCaptor<TelegramLinkToken> captor = ArgumentCaptor.forClass(TelegramLinkToken.class);
        verify(telegramLinkTokenRepository).save(captor.capture());
        TelegramLinkToken saved = captor.getValue();

        assertThat(rawToken).isNotBlank();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 29, 15, 0));
        assertThat(saved.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 3, 29, 15, 10));
        assertThat(saved.getUsedAt()).isNull();

        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
    }

    @Test
    void getTelegramLink_userNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> identityService.getTelegramLink(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");

        verify(userTelegramAccountRepository, never()).findById(any());
    }

    @Test
    void getTelegramLink_telegramNotLinked_returnsUnlinkedResponse() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userTelegramAccountRepository.findById(userId)).thenReturn(Optional.empty());

        TelegramLinkResponse result = identityService.getTelegramLink(userId);

        assertThat(result.isLinked()).isFalse();
        assertThat(result.getChatId()).isNull();
    }

    @Test
    void getTelegramLink_telegramLinked_returnsLinkedResponse() {
        UUID userId = UUID.randomUUID();

        UserTelegramAccount telegramAccount = UserTelegramAccount.builder()
                .userId(userId)
                .chatId(123456789L)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userTelegramAccountRepository.findById(userId))
                .thenReturn(Optional.of(telegramAccount));

        TelegramLinkResponse result = identityService.getTelegramLink(userId);

        assertThat(result.isLinked()).isTrue();
        assertThat(result.getChatId()).isEqualTo(123456789L);
    }

    @Test
    void deleteTelegramLink_userNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> identityService.deleteTelegramLink(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");

        verify(userTelegramAccountRepository, never()).deleteById(any());
    }

    @Test
    void deleteTelegramLink_userExists_deletesTelegramLink() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);

        identityService.deleteTelegramLink(userId);

        verify(userTelegramAccountRepository).deleteById(userId);
    }
}
