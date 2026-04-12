package ru.vladmikhayl.integrations.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import ru.vladmikhayl.integrations.service.AliceOAuthService;

@RestController
@RequestMapping("/api/v1/alice/oauth")
@RequiredArgsConstructor
public class AliceOAuthController {
    private final AliceOAuthService aliceOAuthService;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "state", required = false) String state
    ) {
        aliceOAuthService.validateAuthorizeRequest(responseType, clientId, redirectUri);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("http://localhost:5173/alice/link")
                .queryParam("redirect_uri", redirectUri);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        String location = builder.build(true).toUriString();

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }
}
