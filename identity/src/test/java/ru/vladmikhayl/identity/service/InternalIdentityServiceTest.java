package ru.vladmikhayl.identity.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InternalIdentityServiceTest {
    @Mock
    private UserRepository userRepository;

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
}
