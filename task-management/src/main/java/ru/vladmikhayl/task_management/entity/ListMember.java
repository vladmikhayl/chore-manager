package ru.vladmikhayl.task_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "list_members")
public class ListMember {
    @EmbeddedId
    private ListMemberId id;

    @Column(nullable = false, length = 30)
    private String login;
}
