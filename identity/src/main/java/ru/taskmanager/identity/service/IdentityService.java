package ru.taskmanager.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.taskmanager.identity.dto.request.LoginRequest;
import ru.taskmanager.identity.dto.request.RegisterRequest;
import ru.taskmanager.identity.dto.response.LoginResponse;
import ru.taskmanager.identity.entity.User;
import ru.taskmanager.identity.repository.UserRepository;
import ru.taskmanager.identity.security.JwtService;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new BadCredentialsException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        String token = jwtService.generateToken(user.getId(), user.getLogin());
        return new LoginResponse(token);
    }

    public void register(RegisterRequest registerRequest) {
        String login = registerRequest.getLogin();
        String password = registerRequest.getPassword();

        if (userRepository.existsByLogin(login)) {
            throw new DataIntegrityViolationException("Этот логин уже занят");
        }

        User user = User.builder()
                .login(login)
                .passwordHash(passwordEncoder.encode(password))
                .timezone("Europe/Moscow")
                .dailyReminderEnabled(false)
                .dailyReminderTime(null)
                .build();

        userRepository.save(user);
    }
}
