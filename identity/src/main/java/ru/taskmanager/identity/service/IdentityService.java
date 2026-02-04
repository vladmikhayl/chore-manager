package ru.taskmanager.identity.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.taskmanager.identity.dto.request.LoginRequest;
import ru.taskmanager.identity.dto.request.NotificationSettingsRequest;
import ru.taskmanager.identity.dto.request.RegisterRequest;
import ru.taskmanager.identity.dto.response.LoginResponse;
import ru.taskmanager.identity.dto.response.NotificationSettingsResponse;
import ru.taskmanager.identity.entity.User;
import ru.taskmanager.identity.repository.UserRepository;
import ru.taskmanager.identity.security.JwtService;

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
                .timezoneOffsetHours(3)
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

    public NotificationSettingsResponse getNotificationSettings(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        return new NotificationSettingsResponse(
                user.isDailyReminderEnabled(),
                user.getDailyReminderTime(),
                user.getTimezoneOffsetHours()
        );
    }

    public void updateNotificationSettings(UUID userId, NotificationSettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        Integer timezoneOffsetHours = request.getTimezoneOffsetHours();
        Boolean dailyReminderEnabled = request.getDailyReminderEnabled();
        LocalTime dailyReminderTime = request.getDailyReminderTime();

        if (timezoneOffsetHours != null) {
            user.setTimezoneOffsetHours(timezoneOffsetHours);
        }

        if (dailyReminderTime != null) {
            user.setDailyReminderTime(dailyReminderTime);
        }

        if (dailyReminderEnabled != null) {
            user.setDailyReminderEnabled(dailyReminderEnabled);
        }

        userRepository.save(user);
    }
}
