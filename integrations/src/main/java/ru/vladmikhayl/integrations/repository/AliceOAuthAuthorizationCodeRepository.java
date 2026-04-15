package ru.vladmikhayl.integrations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.integrations.entity.AliceOAuthAuthorizationCode;

import java.util.Optional;
import java.util.UUID;

public interface AliceOAuthAuthorizationCodeRepository extends JpaRepository<AliceOAuthAuthorizationCode, UUID> {
    Optional<AliceOAuthAuthorizationCode> findByCodeHash(String codeHash);
}
