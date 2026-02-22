package ru.vladmikhayl.task_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ListMemberId {
    @Column(name = "list_id", nullable = false)
    private UUID listId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
