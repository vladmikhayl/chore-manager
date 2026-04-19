package ru.vladmikhayl.e2e_tests.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoListMemberResponse {
    private UUID userId;
    private String login;
}
