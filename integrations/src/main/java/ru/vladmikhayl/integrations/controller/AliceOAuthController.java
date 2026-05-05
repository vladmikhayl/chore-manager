package ru.vladmikhayl.integrations.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AliceOAuthController {
    private final AliceOAuthService aliceOAuthService;

    @Value("${alice.oauth.client-id}")
    private String expectedAliceClientId;

    @Value("${alice.link-page-url}")
    private String aliceLinkPageUrl;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam(value = "response_type") String responseType,
            @RequestParam(value = "client_id") String clientId,
            @RequestParam(value = "redirect_uri") String redirectUri,
            @RequestParam(value = "state", required = false) String state
    ) {
        log.info("Called authorization endpoint: client_id={}, response_type={}, redirect_uri={}",
                clientId, responseType, redirectUri);

        if (!"code".equals(responseType)) {
            log.warn("Invalid response_type: {}", responseType);
            return ResponseEntity.badRequest().build();
        }

        if (!expectedAliceClientId.equals(clientId)) {
            log.warn("Invalid client_id: {}", clientId);
            return ResponseEntity.badRequest().build();
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(aliceLinkPageUrl)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_id", clientId);

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
        log.info("Called authorization confirm endpoint");

        if (request.getRedirectUri() == null || request.getRedirectUri().isBlank()) {
            log.warn("Invalid redirect_uri: {}", request.getRedirectUri());
            return ResponseEntity.badRequest().build();
        }

        String code = aliceOAuthService.createAuthorizationCode(userId);

        String redirectUrl = aliceOAuthService.buildRedirectUrl(
                request.getRedirectUri(),
                code,
                request.getState(),
                request.getClientId()
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
        log.info("Called token endpoint");

        return ResponseEntity.ok(
                aliceOAuthService.exchangeCodeToAccessToken(grantType, code, clientId, clientSecret, redirectUri)
        );
    }
}
