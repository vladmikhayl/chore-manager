package ru.vladmikhayl.task_management.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Запрос на создание списка дел")
public class CreateTodoListRequest {
    @NotBlank(message = "Название списка не должно быть пустым")
    @Size(max = 255, message = "Название списка должно быть не длиннее 255 символов")
    @Schema(description = "Название списка дел", example = "Домашние дела")
    private String title;
}
