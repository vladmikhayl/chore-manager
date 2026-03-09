package ru.vladmikhayl.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import ru.vladmikhayl.identity.dto.request.NotificationSettingsRequest;
import ru.vladmikhayl.identity.dto.request.RegisterRequest;
import ru.vladmikhayl.identity.exception.GlobalExceptionHandler;
import ru.vladmikhayl.identity.service.IdentityService;

import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class IdentityControllerTest {
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

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
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
                .andExpect(jsonPath("$.error").exists())
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
                .andExpect(jsonPath("$.error").exists())
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

    @Test
    void updateNotificationSettings_success_returns200() throws Exception {
        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .timezoneOffsetHours(14)
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(10, 0))
                .build();

        doNothing().when(identityService).updateNotificationSettings(any(UUID.class), any(NotificationSettingsRequest.class));

        mockMvc.perform(put("/api/v1/me/notification-settings")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(identityService).updateNotificationSettings(any(UUID.class), any(NotificationSettingsRequest.class));
    }

    @Test
    void updateNotificationSettings_timezoneTooSmall_returns400() throws Exception {
        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .timezoneOffsetHours(-13)
                .build();

        mockMvc.perform(put("/api/v1/me/notification-settings")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(identityService);
    }

    @Test
    void updateNotificationSettings_timezoneTooLarge_returns400() throws Exception {
        NotificationSettingsRequest req = NotificationSettingsRequest.builder()
                .timezoneOffsetHours(15)
                .build();

        mockMvc.perform(put("/api/v1/me/notification-settings")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(identityService);
    }
}
