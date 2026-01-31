package ru.taskmanager.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.taskmanager.identity.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByLogin(String login);

    Optional<User> findByLogin(String login);
}
