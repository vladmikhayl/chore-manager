package ru.vladmikhayl.task_management.entity;

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
}
