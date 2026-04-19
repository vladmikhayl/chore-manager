package ru.vladmikhayl.e2e_tests.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoListShortResponse {
    private UUID id;

    private String title;

    private int membersCount;

    @JsonProperty("isOwner")
    private boolean isOwner;
}
