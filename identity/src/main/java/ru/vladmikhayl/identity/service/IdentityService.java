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
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.repository.UserRepository;
import ru.vladmikhayl.identity.security.JwtService;

import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
                user.isDailyReminderEnabled(),
                user.getDailyReminderTime()
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
}
