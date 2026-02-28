package ru.vladmikhayl.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.vladmikhayl.identity.dto.request.LoginRequest;
import ru.vladmikhayl.identity.dto.request.NotificationSettingsRequest;
import ru.vladmikhayl.identity.dto.request.RegisterRequest;
import ru.vladmikhayl.identity.entity.User;
import ru.vladmikhayl.identity.repository.UserRepository;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = { "spring.config.location=classpath:/application-test.yml" })
@SpringBootTest
@AutoConfigureMockMvc
public class IdentityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        TestPostgresContainer postgres = TestPostgresContainer.getInstance();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Test
    void register_success_returns201_userSavedInDbWithDefaults() throws Exception {
        String login = randomLogin();
        String password = "password";

        RegisterRequest request = RegisterRequest.builder()
                .login(login)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User saved = userRepository.findByLogin(login).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLogin()).isEqualTo(login);
        assertThat(saved.getPasswordHash()).isNotBlank();
        assertThat(saved.getPasswordHash()).isNotEqualTo(password);
        assertThat(saved.getTimezoneOffsetHours()).isEqualTo(3);
        assertThat(saved.isDailyReminderEnabled()).isFalse();
        assertThat(saved.getDailyReminderTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void register_invalidBody_returns400_andUserNotCreated() throws Exception {
        String invalidLogin = "";
        String password = "password";

        RegisterRequest request = RegisterRequest.builder()
                .login(invalidLogin)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByLogin(invalidLogin)).isEmpty();
    }

    @Test
    void register_duplicateLogin_returns409_andDoesNotCreateSecondUser() throws Exception {
        String login = randomLogin();
        String password = "password";

        RegisterRequest request = RegisterRequest.builder()
                .login(login)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertThat(userRepository.findByLogin(login)).isPresent();
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String login = randomLogin();
        String correctPassword = "password";
        String wrongPassword = "wrongpass";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .login(login)
                .password(correctPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .login(login)
                .password(wrongPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_success_returns200_andJwtToken() throws Exception {
        String login = randomLogin();
        String password = "password";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .login(login)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .login(login)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void notificationSettings_getDefaults_thenUpdate_thenGetUpdated() throws Exception {
        String login = randomLogin();
        String password = "password";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .login(login)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        UUID userId = userRepository.findByLogin(login).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/me/notification-settings")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminderEnabled").value(false))
                .andExpect(jsonPath("$.dailyReminderTime").value("08:00:00"))
                .andExpect(jsonPath("$.timezoneOffsetHours").value(3));

        NotificationSettingsRequest updateRequest = NotificationSettingsRequest.builder()
                .dailyReminderEnabled(true)
                .dailyReminderTime(LocalTime.of(10, 0))
                .timezoneOffsetHours(5)
                .build();

        mockMvc.perform(put("/api/v1/me/notification-settings")
                        .header("X-User-Id", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/notification-settings")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminderEnabled").value(true))
                .andExpect(jsonPath("$.dailyReminderTime").value("10:00:00"))
                .andExpect(jsonPath("$.timezoneOffsetHours").value(5));
    }

    @Test
    void internal_getUserLogin_success() throws Exception {
        String login = randomLogin();
        String password = "password";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .login(login)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        UUID userId = userRepository.findByLogin(login).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/internal/users/{userId}/login", userId))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo(login));
    }

    private String randomLogin() {
        return "user_" + UUID.randomUUID().toString().substring(0, 10);
    }
}
