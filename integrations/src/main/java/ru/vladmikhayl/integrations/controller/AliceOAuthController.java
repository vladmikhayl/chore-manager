package ru.vladmikhayl.integrations.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import ru.vladmikhayl.integrations.dto.request.AliceConfirmAuthorizeRequest;
import ru.vladmikhayl.integrations.dto.response.AliceConfirmAuthorizeResponse;
import ru.vladmikhayl.integrations.dto.response.AliceTokenResponse;
import ru.vladmikhayl.integrations.service.AliceOAuthService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alice/oauth")
@RequiredArgsConstructor
@Hidden
public class AliceOAuthController {
    private final AliceOAuthService aliceOAuthService;

    @Value("${alice.link-page-url}")
    private String aliceLinkPageUrl;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "state", required = false) String state
    ) {
//        aliceOAuthService.validateAuthorizeRequest(responseType, clientId, redirectUri);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(aliceLinkPageUrl)
                .queryParam("redirect_uri", redirectUri);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        String location = builder.build(true).toUriString();

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }

    @PostMapping("/authorize/confirm")
    public ResponseEntity<AliceConfirmAuthorizeResponse> confirmAuthorize(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody AliceConfirmAuthorizeRequest request
    ) {
        aliceOAuthService.validateConfirmRequest(request.getRedirectUri());

        String code = aliceOAuthService.createAuthorizationCode(userId);

        String redirectUrl = aliceOAuthService.buildRedirectUrl(
                request.getRedirectUri(),
                code,
                request.getState()
        );

        return ResponseEntity.ok(new AliceConfirmAuthorizeResponse(redirectUrl));
    }

    @PostMapping(
            value = "/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<AliceTokenResponse> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("code") String code,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("redirect_uri") String redirectUri
    ) {
        return ResponseEntity.ok(
                aliceOAuthService.exchangeCodeToAccessToken(grantType, code, clientId, clientSecret, redirectUri)
        );
    }
}
