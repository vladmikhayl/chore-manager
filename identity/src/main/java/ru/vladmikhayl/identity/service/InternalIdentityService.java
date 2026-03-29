package ru.vladmikhayl.identity.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vladmikhayl.identity.dto.request.TelegramLinkRequest;
import ru.vladmikhayl.identity.entity.TelegramLinkToken;
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.entity.UserTelegramAccount;
import ru.vladmikhayl.identity.repository.TelegramLinkTokenRepository;
import ru.vladmikhayl.identity.repository.UserRepository;
import ru.vladmikhayl.identity.repository.UserTelegramAccountRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalIdentityService {
    private final TelegramLinkTokenRepository telegramLinkTokenRepository;
    private final UserTelegramAccountRepository userTelegramAccountRepository;
    private final UserRepository userRepository;
    private final HashService hashService;
    private final Clock clock;

    @Transactional
    public String getLoginById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        return user.getLogin();
    }

    @Transactional
    public void linkTelegramAccount(TelegramLinkRequest request) {
        String tokenHash = hashService.sha256(request.getToken());

        TelegramLinkToken telegramLinkToken = telegramLinkTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Токен привязки недействителен"));

        if (telegramLinkToken.getUsedAt() != null) {
            throw new BadCredentialsException("Токен привязки уже использован");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (!telegramLinkToken.getExpiresAt().isAfter(now)) {
            throw new BadCredentialsException("Срок действия токена привязки истёк");
        }

        UserTelegramAccount userTelegramAccount = UserTelegramAccount.builder()
                .userId(telegramLinkToken.getUserId())
                .chatId(request.getChatId())
                .build();

        userTelegramAccountRepository.save(userTelegramAccount);

        telegramLinkToken.setUsedAt(now);
        telegramLinkTokenRepository.save(telegramLinkToken);
    }
}
