package ru.taskmanager.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.taskmanager.identity.dto.request.LoginRequest;
import ru.taskmanager.identity.dto.request.NotificationSettingsRequest;
import ru.taskmanager.identity.dto.request.RegisterRequest;
import ru.taskmanager.identity.dto.response.LoginResponse;
import ru.taskmanager.identity.dto.response.NotificationSettingsResponse;
import ru.taskmanager.identity.service.IdentityService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IdentityController {
    private final IdentityService identityService;

    @PostMapping("/auth/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        identityService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(identityService.login(request));
    }

    @GetMapping("/me/notification-settings")
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        NotificationSettingsResponse response = identityService.getNotificationSettings(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/notification-settings")
    public ResponseEntity<Void> updateNotificationSettings(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody NotificationSettingsRequest request
    ) {
        identityService.updateNotificationSettings(userId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
