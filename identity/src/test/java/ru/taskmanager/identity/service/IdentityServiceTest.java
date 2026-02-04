package ru.taskmanager.identity.service;

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
import ru.taskmanager.identity.dto.request.LoginRequest;
import ru.taskmanager.identity.dto.request.NotificationSettingsRequest;
import ru.taskmanager.identity.dto.request.RegisterRequest;
import ru.taskmanager.identity.dto.response.LoginResponse;
import ru.taskmanager.identity.dto.response.NotificationSettingsResponse;
import ru.taskmanager.identity.entity.User;
import ru.taskmanager.identity.repository.UserRepository;
import ru.taskmanager.identity.security.JwtService;

import java.time.LocalTime;
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

    @InjectMocks
    private IdentityService identityService;

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
        assertThat(saved.getTimezoneOffsetHours()).isEqualTo(3);
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
    void getNotificationSettings_success_returnsSettings() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .timezoneOffsetHours(-10)
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(9, 0))
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        NotificationSettingsResponse response = identityService.getNotificationSettings(userId);

        assertThat(response.getTimezoneOffsetHours()).isEqualTo(-10);
        assertThat(response.isDailyReminderEnabled()).isTrue();
        assertThat(response.getDailyReminderTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void getNotificationSettings_userNotFound_throwsEntityNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityService.getNotificationSettings(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    void updateNotificationSettings_userNotFound_throwsEntityNotFound() {
        UUID userId = UUID.randomUUID();

        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .timezoneOffsetHours(5)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityService.updateNotificationSettings(userId, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateNotificationSettings_onlyTimezone_updatesOffsetAndDoesNotTouchOthers() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .timezoneOffsetHours(3)
                .dailyReminderEnabled(false)
                .dailyReminderTime(LocalTime.of(8, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .timezoneOffsetHours(-10)
                .build();

        identityService.updateNotificationSettings(userId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getTimezoneOffsetHours()).isEqualTo(-10);
        assertThat(saved.isDailyReminderEnabled()).isFalse();
        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void updateNotificationSettings_onlyTime_updatesTimeAndDoesNotTouchOthers() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .timezoneOffsetHours(3)
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
        assertThat(saved.getTimezoneOffsetHours()).isEqualTo(3);
    }

    @Test
    void updateNotificationSettings_onlyEnabled_updatesEnabledAndDoesNotTouchOthers() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .timezoneOffsetHours(-5)
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
        assertThat(saved.getTimezoneOffsetHours()).isEqualTo(-5);
    }

    @Test
    void updateNotificationSettings_allFields_updatesAll() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .timezoneOffsetHours(3)
                .dailyReminderEnabled(false)
                .dailyReminderTime(LocalTime.of(8, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .timezoneOffsetHours(5)
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(7, 0))
                .build();

        identityService.updateNotificationSettings(userId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getTimezoneOffsetHours()).isEqualTo(5);
        assertThat(saved.isDailyReminderEnabled()).isTrue();
        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    void updateNotificationSettings_emptyRequest_doesNotChangeAnything() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .timezoneOffsetHours(3)
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(8, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotificationSettingsRequest req = NotificationSettingsRequest.builder().build();

        identityService.updateNotificationSettings(userId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getTimezoneOffsetHours()).isEqualTo(3);
        assertThat(saved.isDailyReminderEnabled()).isTrue();
        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(8, 0));
    }
}
