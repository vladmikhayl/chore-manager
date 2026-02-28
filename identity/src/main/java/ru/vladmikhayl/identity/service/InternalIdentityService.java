package ru.vladmikhayl.identity.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalIdentityService {
    private final UserRepository userRepository;

    @Transactional
    public String getLoginById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        return user.getLogin();
    }
}
