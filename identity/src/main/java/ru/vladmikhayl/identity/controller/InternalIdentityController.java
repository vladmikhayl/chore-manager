package ru.vladmikhayl.identity.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladmikhayl.identity.dto.request.TelegramLinkRequest;
import ru.vladmikhayl.identity.service.InternalIdentityService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Hidden
public class InternalIdentityController {
    private final InternalIdentityService internalIdentityService;

    @GetMapping("/users/{userId}/login")
    public ResponseEntity<String> getUserLogin(@PathVariable UUID userId) {
        String login = internalIdentityService.getLoginById(userId);
        return ResponseEntity.ok(login);
    }

    @PostMapping("/telegram/link")
    public ResponseEntity<Void> linkTelegramAccount(
            @Valid @RequestBody TelegramLinkRequest request
    ) {
        internalIdentityService.linkTelegramAccount(request);
        return ResponseEntity.ok().build();
    }
}
