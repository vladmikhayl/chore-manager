package ru.taskmanager.identity.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.taskmanager.identity.dto.request.RegisterRequest;
import ru.taskmanager.identity.exception.GlobalExceptionHandler;
import ru.taskmanager.identity.service.IdentityService;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class IdentityControllerTests {
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private IdentityService identityService;

    @InjectMocks
    private IdentityController identityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(identityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_success_returns201() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .login("username")
                .password("password")
                .build();

        doNothing().when(identityService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        verify(identityService).register(any(RegisterRequest.class));
    }

    @Test
    void register_blankLogin_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .login("")
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(identityService);
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .login("username")
                .password("123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(identityService);
    }

    @Test
    void register_loginTaken_returns409() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .login("username")
                .password("password")
                .build();

        doThrow(new DataIntegrityViolationException("Этот логин уже занят"))
                .when(identityService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Этот логин уже занят"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
