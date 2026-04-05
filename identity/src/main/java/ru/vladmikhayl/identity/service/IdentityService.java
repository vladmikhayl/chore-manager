package ru.vladmikhayl.identity.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.vladmikhayl.identity.dto.request.LoginRequest;
import ru.vladmikhayl.identity.dto.request.NotificationSettingsRequest;
import ru.vladmikhayl.identity.dto.request.RegisterRequest;
import ru.vladmikhayl.identity.dto.response.LoginResponse;
import ru.vladmikhayl.identity.dto.response.ProfileResponse;
import ru.vladmikhayl.identity.entity.TelegramLinkToken;
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.repository.TelegramLinkTokenRepository;
import ru.vladmikhayl.identity.repository.UserRepository;
import ru.vladmikhayl.identity.security.JwtService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserRepository userRepository;
    private final TelegramLinkTokenRepository telegramLinkTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final HashService hashService;
    private final Clock clock;

    public void register(RegisterRequest registerRequest) {
        String login = registerRequest.getLogin();
        String password = registerRequest.getPassword();

        if (userRepository.existsByLogin(login)) {
            throw new DataIntegrityViolationException("Этот логин уже занят");
        }

        User user = User.builder()
                .login(login)
                .passwordHash(passwordEncoder.encode(password))
                .dailyReminderEnabled(false)
                .dailyReminderTime(LocalTime.of(8, 0))
                .build();

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new BadCredentialsException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        String token = jwtService.generateToken(user.getId(), user.getLogin());
        return new LoginResponse(token);
    }

    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        return new ProfileResponse(
                user.getLogin(),
                user.isDailyReminderEnabled()
        );
    }

    public void updateNotificationSettings(UUID userId, NotificationSettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        Boolean dailyReminderEnabled = request.getDailyReminderEnabled();
        LocalTime dailyReminderTime = request.getDailyReminderTime();

        if (dailyReminderTime != null) {
            user.setDailyReminderTime(dailyReminderTime);
        }

        if (dailyReminderEnabled != null) {
            user.setDailyReminderEnabled(dailyReminderEnabled);
        }

        userRepository.save(user);
    }

    public String createTelegramLinkToken(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Пользователь не найден");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusMinutes(10);

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashService.sha256(rawToken);

        TelegramLinkToken telegramLinkToken = TelegramLinkToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .usedAt(null)
                .build();

        telegramLinkTokenRepository.save(telegramLinkToken);

        return rawToken;
    }
}
