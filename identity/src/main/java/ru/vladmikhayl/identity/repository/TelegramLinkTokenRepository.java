package ru.vladmikhayl.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.identity.entity.TelegramLinkToken;

import java.util.Optional;
import java.util.UUID;

public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, UUID> {
    Optional<TelegramLinkToken> findByTokenHash(String tokenHash);
}