package ru.taskmanager.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.taskmanager.identity.dto.request.RegisterRequest;
import ru.taskmanager.identity.entity.User;
import ru.taskmanager.identity.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .build();

        userRepository.save(user);
    }
}
