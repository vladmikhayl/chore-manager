package ru.taskmanager.identity.service;

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
import ru.taskmanager.identity.dto.request.RegisterRequest;
import ru.taskmanager.identity.dto.response.LoginResponse;
import ru.taskmanager.identity.entity.User;
import ru.taskmanager.identity.repository.UserRepository;
import ru.taskmanager.identity.security.JwtService;

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
        assertThat(saved.getTimezone()).isEqualTo("Europe/Moscow");
        assertThat(saved.isDailyReminderEnabled()).isFalse();
        assertThat(saved.getDailyReminderTime()).isNull();
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
}
