package ru.vladmikhayl.e2e_tests.scenario;

import org.junit.jupiter.api.Test;
import ru.vladmikhayl.e2e_tests.BaseE2ETest;
import ru.vladmikhayl.e2e_tests.dto.response.LoginResponse;
import ru.vladmikhayl.e2e_tests.dto.response.ProfileResponse;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AuthFlowE2ETests extends BaseE2ETest {
    @Test
    void testRegisterLoginAndAccessProtectedEndpoint() {
        // Создаём пользователя
        String login = generateRandomLogin();

        authHelper.register(login, DEFAULT_PASSWORD);

        // Логинимся
        LoginResponse loginResponse = authHelper.login(login, DEFAULT_PASSWORD);

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getToken()).isNotBlank();

        // Получаем профиль
        ProfileResponse profileResponse = authHelper.getProfile(loginResponse.getToken());

        assertThat(profileResponse).isNotNull();
        assertThat(profileResponse.getLogin()).isEqualTo(login);
        assertThat(profileResponse.isDailyReminderEnabled()).isFalse();
    }

    @Test
    void testRequestWithWrongTokenReturnsUnauthorized() {
        assertThatThrownBy(() -> authHelper.getProfile("wrong-token"))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void testLoginWithWrongPasswordReturnsUnauthorized() {
        // Создаём пользователя
        String login = generateRandomLogin();

        authHelper.register(login, DEFAULT_PASSWORD);

        // Пытаемся войти с неверным паролем
        assertThatThrownBy(() -> authHelper.login(login, "wrong-password"))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }
}
