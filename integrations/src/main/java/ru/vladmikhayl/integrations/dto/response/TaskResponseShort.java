package ru.vladmikhayl.integrations.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Короткая информация о задаче")
public class TaskResponseShort {
    @Schema(description = "ID задачи")
    private UUID id;

    @Schema(description = "Название списка дел")
    private String listTitle;

    @Schema(description = "Название задачи", example = "Вынести мусор")
    private String title;
}
