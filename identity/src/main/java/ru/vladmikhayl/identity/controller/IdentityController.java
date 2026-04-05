package ru.vladmikhayl.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladmikhayl.identity.dto.request.LoginRequest;
import ru.vladmikhayl.identity.dto.request.NotificationSettingsRequest;
import ru.vladmikhayl.identity.dto.request.RegisterRequest;
import ru.vladmikhayl.identity.dto.response.LoginResponse;
import ru.vladmikhayl.identity.dto.response.ProfileResponse;
import ru.vladmikhayl.identity.dto.response.TelegramLinkResponse;
import ru.vladmikhayl.identity.service.IdentityService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Identity", description = "Эндпоинты для работы с профилями")
public class IdentityController {
    private final IdentityService identityService;

    @PostMapping("/auth/register")
    @Operation(summary = "Зарегистрироваться")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Успешная регистрация"),
            @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким логином уже существует", content = @Content)
    })
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        identityService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/auth/login")
    @Operation(
            summary = "Аутентифицироваться",
            description = "Проверяется введенный логин и пароль, и при верных данных выдается JWT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль", content = @Content)
    })
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(identityService.login(request));
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Получить данные профиля текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProfileResponse> getProfile(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId
    ) {
        ProfileResponse response = identityService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/telegram-link")
    @Operation(summary = "Получить информацию о привязке Telegram текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TelegramLinkResponse> getTelegramLink(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId
    ) {
        TelegramLinkResponse response = identityService.getTelegramLink(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/telegram-link")
    @Operation(summary = "Отвязать Telegram текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteTelegramLink(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId
    ) {
        identityService.deleteTelegramLink(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/notification-settings")
    @Operation(summary = "Изменить информацию о настройках напоминаний")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
            @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> updateNotificationSettings(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId,
            @Valid @RequestBody NotificationSettingsRequest request
    ) {
        identityService.updateNotificationSettings(userId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/me/telegram/link-token")
    @Operation(summary = "Создать одноразовый токен для привязки Telegram")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> createTelegramLinkToken(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) UUID userId
    ) {
        String response = identityService.createTelegramLinkToken(userId);
        return ResponseEntity.ok(response);
    }
}
