package ru.vladmikhayl.task_management.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Подробная информация о списке дел")
public class TodoListDetailsResponse {
    @Schema(description = "Идентификатор списка дел")
    private UUID id;

    @Schema(
            description = "Название списка дел",
            example = "Домашние дела"
    )
    private String title;

    @Schema(description = "Идентификатор создателя списка дел")
    private UUID ownerUserId;

    @JsonProperty("isOwner")
    @Schema(
            description = "Является ли текущий пользователь создателем списка",
            example = "true"
    )
    private boolean isOwner;

    @Schema(description = "Участники списка")
    private List<TodoListMemberResponse> members;
}
