package ru.vladmikhayl.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.identity.entity.UserTelegramAccount;

import java.util.UUID;

public interface UserTelegramAccountRepository extends JpaRepository<UserTelegramAccount, UUID> {
}
