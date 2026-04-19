package ru.vladmikhayl.e2e_tests.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.vladmikhayl.e2e_tests.dto.request.CreateTaskRequest;
import ru.vladmikhayl.e2e_tests.dto.response.TaskCompletionStatusResponse;
import ru.vladmikhayl.e2e_tests.dto.response.TaskResponse;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TaskHelper {
    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public UUID createTask(String token, UUID listId, CreateTaskRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreateTaskRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<UUID> response = restTemplate.exchange(
                gatewayUrl + "/lists/" + listId + "/tasks",
                HttpMethod.POST,
                entity,
                UUID.class
        );

        return response.getBody();
    }

    public List<TaskResponse> getTasks(String token, UUID listId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<TaskResponse>> response = restTemplate.exchange(
                gatewayUrl + "/lists/" + listId + "/tasks",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public List<TaskResponse> getTasksForDay(String token, String date) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<TaskResponse>> response = restTemplate.exchange(
                gatewayUrl + "/tasks?date=" + date,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public void completeTask(String token, UUID taskId, String date) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(
                gatewayUrl + "/tasks/" + taskId + "/completions/" + date,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    public TaskCompletionStatusResponse getTaskCompletion(String token, UUID taskId, String date) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<TaskCompletionStatusResponse> response = restTemplate.exchange(
                gatewayUrl + "/tasks/" + taskId + "/completions/" + date,
                HttpMethod.GET,
                entity,
                TaskCompletionStatusResponse.class
        );

        return response.getBody();
    }

    public void deleteTaskCompletion(String token, UUID taskId, String date) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(
                gatewayUrl + "/tasks/" + taskId + "/completions/" + date,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
    }
}
