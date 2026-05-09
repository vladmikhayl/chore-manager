package ru.vladmikhayl.integrations.feign;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladmikhayl.integrations.config.FeignConfig;
import ru.vladmikhayl.integrations.dto.request.TelegramLinkRequest;
import ru.vladmikhayl.integrations.dto.response.TaskCompletionStatusResponse;
import ru.vladmikhayl.integrations.dto.response.TaskResponseShort;

import java.util.List;
import java.util.UUID;

@Profile("!test")
@org.springframework.cloud.openfeign.FeignClient(name = "gateway", configuration = FeignConfig.class)
public interface FeignClient {
    @PostMapping("/api/v1/internal/telegram/link")
    ResponseEntity<Void> linkTelegramAccount(@RequestBody TelegramLinkRequest request);

    @GetMapping("/api/v1/internal/tasks")
    List<TaskResponseShort> getTasksForDay(@RequestHeader("X-User-Id") UUID userId, @RequestParam String date);

    @PutMapping("/api/v1/internal/tasks/{taskId}/completions/{date}")
    ResponseEntity<Void> completeTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId,
            @PathVariable String date
    );

    @GetMapping("/api/v1/internal/tasks/{taskId}/completions/{date}")
    TaskCompletionStatusResponse getTaskCompletion(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId,
            @PathVariable String date
    );
}
