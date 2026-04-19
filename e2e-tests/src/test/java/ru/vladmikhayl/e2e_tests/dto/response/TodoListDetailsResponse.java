package ru.vladmikhayl.e2e_tests.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoListDetailsResponse {
    private UUID id;

    private String title;

    private UUID ownerUserId;

    @JsonProperty("isOwner")
    private boolean isOwner;

    private List<TodoListMemberResponse> members;
}
