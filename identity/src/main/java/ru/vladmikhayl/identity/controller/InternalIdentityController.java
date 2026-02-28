package ru.vladmikhayl.identity.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
