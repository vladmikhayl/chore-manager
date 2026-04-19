package ru.vladmikhayl.e2e_tests.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.vladmikhayl.e2e_tests.dto.request.AcceptInviteRequest;
import ru.vladmikhayl.e2e_tests.dto.request.CreateTodoListRequest;
import ru.vladmikhayl.e2e_tests.dto.response.CreateInviteResponse;
import ru.vladmikhayl.e2e_tests.dto.response.TodoListDetailsResponse;
import ru.vladmikhayl.e2e_tests.dto.response.TodoListShortResponse;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListHelper {
    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public void createList(String token, String title) {
        CreateTodoListRequest request = new CreateTodoListRequest(title);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreateTodoListRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange(
                gatewayUrl + "/lists",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public List<TodoListShortResponse> getLists(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<TodoListShortResponse>> response = restTemplate.exchange(
                gatewayUrl + "/lists",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public TodoListDetailsResponse getListDetails(String token, UUID listId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<TodoListDetailsResponse> response = restTemplate.exchange(
                gatewayUrl + "/lists/" + listId,
                HttpMethod.GET,
                entity,
                TodoListDetailsResponse.class
        );

        return response.getBody();
    }

    public CreateInviteResponse createInvite(String token, UUID listId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CreateInviteResponse> response = restTemplate.exchange(
                gatewayUrl + "/lists/" + listId + "/invites",
                HttpMethod.POST,
                entity,
                CreateInviteResponse.class
        );

        return response.getBody();
    }

    public void acceptInvite(String token, String inviteToken) {
        AcceptInviteRequest request = new AcceptInviteRequest(inviteToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AcceptInviteRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange(
                gatewayUrl + "/invites/accept",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }
}
