package ru.vladmikhayl.task_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vladmikhayl.task_management.entity.ListMember;
import ru.vladmikhayl.task_management.entity.ListMemberId;

import java.util.List;
import java.util.UUID;

public interface ListMemberRepository extends JpaRepository<ListMember, ListMemberId> {
    int countById_ListId(UUID listId);

    boolean existsById_ListIdAndId_UserId(UUID listId, UUID userId);

    List<ListMember> findAllById_UserId(UUID userId);
}
