package ru.vladmikhayl.e2e_tests.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.vladmikhayl.e2e_tests.dto.request.LoginRequest;
import ru.vladmikhayl.e2e_tests.dto.request.RegisterRequest;
import ru.vladmikhayl.e2e_tests.dto.response.LoginResponse;
import ru.vladmikhayl.e2e_tests.dto.response.ProfileResponse;

@RequiredArgsConstructor
public class AuthHelper {
    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public void register(String login, String password) {
        RegisterRequest request = new RegisterRequest(login, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange(
                gatewayUrl + "/auth/register",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public LoginResponse login(String login, String password) {
        LoginRequest request = new LoginRequest(login, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<LoginResponse> response = restTemplate.exchange(
                gatewayUrl + "/auth/login",
                HttpMethod.POST,
                entity,
                LoginResponse.class
        );

        return response.getBody();
    }

    public ProfileResponse getProfile(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ProfileResponse> response = restTemplate.exchange(
                gatewayUrl + "/me/profile",
                HttpMethod.GET,
                entity,
                ProfileResponse.class
        );

        return response.getBody();
    }
}
