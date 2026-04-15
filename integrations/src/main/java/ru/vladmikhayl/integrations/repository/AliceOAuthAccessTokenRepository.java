package ru.vladmikhayl.integrations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.integrations.entity.AliceOAuthAccessToken;

import java.util.Optional;
import java.util.UUID;

public interface AliceOAuthAccessTokenRepository extends JpaRepository<AliceOAuthAccessToken, UUID> {
    Optional<AliceOAuthAccessToken> findByTokenHash(String tokenHash);
}
