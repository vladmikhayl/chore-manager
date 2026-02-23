package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.ListInvite;

import java.util.Optional;
import java.util.UUID;

public interface ListInviteRepository extends JpaRepository<ListInvite, UUID> {
    Optional<ListInvite> findByToken(String token);
}
