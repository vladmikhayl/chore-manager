package ru.vladmikhayl.integrations.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladmikhayl.integrations.dto.request.AliceRequest;
import ru.vladmikhayl.integrations.dto.response.AliceResponse;
import ru.vladmikhayl.integrations.dto.response.TaskCompletionStatusResponse;
import ru.vladmikhayl.integrations.dto.response.TaskResponseShort;
import ru.vladmikhayl.integrations.entity.AliceOAuthAccessToken;
import ru.vladmikhayl.integrations.feign.FeignClient;
import ru.vladmikhayl.integrations.repository.AliceOAuthAccessTokenRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AliceServiceTest {
    @Mock
    private FeignClient feignClient;

    @Mock
    private HashService hashService;

    @Mock
    private AliceOAuthAccessTokenRepository accessTokenRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private AliceService aliceService;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void handleWebhook_noToken_returnsStartAccountLinking() {
        AliceRequest request = buildRequest("какие у меня задачи на сегодня");

        AliceResponse response = aliceService.handleWebhook(request, null);

        assertThat(response.getResponse()).isNull();
        assertThat(response.getStart_account_linking()).isEmpty();
        assertThat(response.getVersion()).isEqualTo("1.0");

        verifyNoInteractions(hashService, accessTokenRepository, feignClient);
    }

    @Test
    void handleWebhook_tokenNotFound_returnsStartAccountLinking() {
        AliceRequest request = buildRequest("какие у меня задачи на сегодня");

        when(hashService.sha256("header-token")).thenReturn("header-hash");
        when(accessTokenRepository.findByTokenHash("header-hash")).thenReturn(Optional.empty());

        AliceResponse response = aliceService.handleWebhook(request, "Bearer header-token");

        assertThat(response.getResponse()).isNull();
        assertThat(response.getStart_account_linking()).isEmpty();

        verify(hashService).sha256("header-token");
        verify(accessTokenRepository).findByTokenHash("header-hash");
        verifyNoInteractions(feignClient);
    }

    @Test
    void handleWebhook_expiredToken_returnsStartAccountLinking() {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Moscow"));
        when(clock.instant()).thenReturn(Instant.parse("2026-04-21T09:00:00Z"));

        AliceRequest request = buildRequest("какие у меня задачи на сегодня");

        AliceOAuthAccessToken expiredToken = AliceOAuthAccessToken.builder()
                .userId(userId)
                .tokenHash("expired-hash")
                .expiresAt(LocalDateTime.of(2026, 4, 21, 11, 59, 59))
                .build();

        when(hashService.sha256("expired-token")).thenReturn("expired-hash");
        when(accessTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expiredToken));

        AliceResponse response = aliceService.handleWebhook(request, "Bearer expired-token");

        assertThat(response.getResponse()).isNull();
        assertThat(response.getStart_account_linking()).isEmpty();

        verify(hashService).sha256("expired-token");
        verify(accessTokenRepository).findByTokenHash("expired-hash");
        verifyNoInteractions(feignClient);
    }

    @Test
    void handleWebhook_validBearerToken_todayCommand_returnsTasks() {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Moscow"));
        when(clock.instant()).thenReturn(Instant.parse("2026-04-21T09:00:00Z"));

        AliceRequest request = buildRequest("какие у меня задачи на сегодня");

        AliceOAuthAccessToken validToken = AliceOAuthAccessToken.builder()
                .userId(userId)
                .tokenHash("valid-hash")
                .expiresAt(LocalDateTime.of(2026, 4, 21, 12, 0, 1))
                .build();

        when(hashService.sha256("valid-token")).thenReturn("valid-hash");
        when(accessTokenRepository.findByTokenHash("valid-hash")).thenReturn(Optional.of(validToken));
        when(feignClient.getTasksForDay(userId, "2026-04-21"))
                .thenReturn(List.of(
                        TaskResponseShort.builder().title("Вынести мусор").build(),
                        TaskResponseShort.builder().title("Купить продукты").build()
                ));

        AliceResponse response = aliceService.handleWebhook(request, "Bearer valid-token");

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().isEnd_session()).isFalse();
        assertThat(response.getResponse().getText())
                .isEqualTo("На сегодня у вас две задачи: вынести мусор, купить продукты.");

        verify(hashService).sha256("valid-token");
        verify(accessTokenRepository).findByTokenHash("valid-hash");
        verify(feignClient).getTasksForDay(userId, "2026-04-21");
    }

    @Test
    void handleWebhook_validOAuthToken_tomorrowCommand_returnsTasks() {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Moscow"));
        when(clock.instant()).thenReturn(Instant.parse("2026-04-21T09:00:00Z"));

        AliceRequest request = buildRequest("какие задачи на завтра");

        AliceOAuthAccessToken validToken = AliceOAuthAccessToken.builder()
                .userId(userId)
                .tokenHash("oauth-hash")
                .expiresAt(LocalDateTime.of(2026, 4, 21, 12, 0, 1))
                .build();

        when(hashService.sha256("oauth-token")).thenReturn("oauth-hash");
        when(accessTokenRepository.findByTokenHash("oauth-hash")).thenReturn(Optional.of(validToken));
        when(feignClient.getTasksForDay(userId, "2026-04-22"))
                .thenReturn(List.of(
                        TaskResponseShort.builder().title("Помыть посуду").build()
                ));

        AliceResponse response = aliceService.handleWebhook(request, "OAuth oauth-token");

        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .isEqualTo("На завтра у вас одна задача: помыть посуду.");

        verify(hashService).sha256("oauth-token");
        verify(accessTokenRepository).findByTokenHash("oauth-hash");
        verify(feignClient).getTasksForDay(userId, "2026-04-22");
    }

    @Test
    void handleWebhook_validTokenInBody_returnsTasks() {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Moscow"));
        when(clock.instant()).thenReturn(Instant.parse("2026-04-21T09:00:00Z"));

        AliceRequest request = buildRequestWithBodyToken("какие у меня задачи на сегодня", "body-token");

        AliceOAuthAccessToken validToken = AliceOAuthAccessToken.builder()
                .userId(userId)
                .tokenHash("body-hash")
                .expiresAt(LocalDateTime.of(2026, 4, 21, 12, 0, 1))
                .build();

        when(hashService.sha256("body-token")).thenReturn("body-hash");
        when(accessTokenRepository.findByTokenHash("body-hash")).thenReturn(Optional.of(validToken));
        when(feignClient.getTasksForDay(userId, "2026-04-21"))
                .thenReturn(List.of(TaskResponseShort.builder().title("Протереть пыль").build()));

        AliceResponse response = aliceService.handleWebhook(request, null);

        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .isEqualTo("На сегодня у вас одна задача: протереть пыль.");

        verify(hashService).sha256("body-token");
        verify(accessTokenRepository).findByTokenHash("body-hash");
        verify(feignClient).getTasksForDay(userId, "2026-04-21");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "отметь выполненной задачу вынести мусор",
            "отметить выполненный задачу вынести мусор",
            "отметьте выполненным вынести мусор",
            "отметь задачу вынести мусор выполненный",
            "отметить задача вынести мусор выполнена",
            "отметьте вынести мусор выполненным",
            "задачу вынести мусор отметь выполненной",
            "задача вынести мусор отметить выполненную",
            "вынести мусор отметьте выполненным",
            "отметь задачу выполненной вынести мусор",
            "отметь задачу вынести мусор",
            "отметь вынести мусор",
            "задачу вынести мусор отметь",
            "вынести мусор отметь"
    })
    void handleWebhook_completeTaskCommand_validPhrases_completesTask(String command) {
        mockValidToken("valid-token", "valid-hash");

        UUID taskId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        AliceRequest request = buildRequest(command);

        when(feignClient.getTasksForDay(userId, "2026-04-21"))
                .thenReturn(List.of(
                        TaskResponseShort.builder()
                                .id(taskId)
                                .title("Вынести мусор")
                                .build()
                ));

        when(feignClient.getTaskCompletion(userId, taskId, "2026-04-21"))
                .thenReturn(TaskCompletionStatusResponse.builder()
                        .completed(false)
                        .build());

        AliceResponse response = aliceService.handleWebhook(request, "Bearer valid-token");

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .isIn(
                        "Готово! Задача отмечена выполненной.",
                        "Отметила выполненной. Так держать!",
                        "Отметила. Хорошая работа!",
                        "Готово, отметила."
                );

        verify(hashService).sha256("valid-token");
        verify(accessTokenRepository).findByTokenHash("valid-hash");
        verify(feignClient).getTasksForDay(userId, "2026-04-21");
        verify(feignClient).completeTask(userId, taskId, "2026-04-21");
    }

    @Test
    void handleWebhook_completeTaskCommand_alreadyCompleted_returnsAlreadyCompletedMessage() {
        mockValidToken("valid-token", "valid-hash");

        UUID taskId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        AliceRequest request = buildRequest("отметь выполненной задачу вынести мусор");

        when(feignClient.getTasksForDay(userId, "2026-04-21"))
                .thenReturn(List.of(
                        TaskResponseShort.builder()
                                .id(taskId)
                                .title("Вынести мусор")
                                .build()
                ));

        when(feignClient.getTaskCompletion(userId, taskId, "2026-04-21"))
                .thenReturn(TaskCompletionStatusResponse.builder()
                        .completed(true)
                        .build());

        AliceResponse response = aliceService.handleWebhook(request, "Bearer valid-token");

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .isIn(
                        "Кажется, эта задача уже отмечена выполненной.",
                        "Уже отмечено — повторно ничего менять не пришлось."
                );

        verify(feignClient, never()).completeTask(any(), any(), anyString());
    }

    @Test
    void handleWebhook_completeTaskCommand_invalidPhrase_returnsFormatHint() {
        mockValidToken("valid-token", "valid-hash");

        AliceRequest request = buildRequest("выполнил вынести мусор");

        AliceResponse response = aliceService.handleWebhook(request, "Bearer valid-token");

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .isEqualTo("Чтобы отметить задачу выполненной, скажите, например: отметь задачу \"вынести мусор\".");

        verify(hashService).sha256("valid-token");
        verify(accessTokenRepository).findByTokenHash("valid-hash");
        verifyNoInteractions(feignClient);
    }

    @Test
    void handleWebhook_completeTaskCommand_taskNotFound_returnsMessage() {
        mockValidToken("valid-token", "valid-hash");

        AliceRequest request = buildRequest("отметь выполненной задачу вынести мусор");

        when(feignClient.getTasksForDay(userId, "2026-04-21"))
                .thenReturn(List.of(
                        TaskResponseShort.builder()
                                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                                .title("Купить продукты")
                                .build()
                ));

        AliceResponse response = aliceService.handleWebhook(request, "Bearer valid-token");

        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .isIn(
                        "Кажется, на сегодня у вас нет такой задачи.",
                        "Не нашла такую задачу на сегодня.",
                        "Похоже, сегодня такой задачи нет."
                );

        verify(feignClient).getTasksForDay(userId, "2026-04-21");
        verify(feignClient, never()).completeTask(any(), any(), anyString());
    }

    @Test
    void handleWebhook_completeTaskCommand_multipleMatchingTasks_returnsAmbiguousMessage() {
        mockValidToken("valid-token", "valid-hash");

        AliceRequest request = buildRequest("отметь выполненной задачу вынести мусор");

        when(feignClient.getTasksForDay(userId, "2026-04-21"))
                .thenReturn(List.of(
                        TaskResponseShort.builder()
                                .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                                .title("Вынести мусор")
                                .build(),
                        TaskResponseShort.builder()
                                .id(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                                .title("вынести мусор")
                                .build()
                ));

        AliceResponse response = aliceService.handleWebhook(request, "Bearer valid-token");

        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .isEqualTo("На сегодня нашлось несколько задач с таким названием. К сожалению, пока я не могу отмечать задачи в таком случае.");

        verify(feignClient).getTasksForDay(userId, "2026-04-21");
        verify(feignClient, never()).completeTask(any(), any(), anyString());
    }

    @Test
    void handleWebhook_completeTaskCommandWithoutToken_returnsStartAccountLinking() {
        AliceRequest request = buildRequest("отметь выполненной задачу вынести мусор");

        AliceResponse response = aliceService.handleWebhook(request, null);

        assertThat(response.getResponse()).isNull();
        assertThat(response.getStart_account_linking()).isEmpty();
        assertThat(response.getVersion()).isEqualTo("1.0");

        verifyNoInteractions(hashService, accessTokenRepository, feignClient);
    }

    @Test
    void handleWebhook_startCommand_returnsReferenceHelpWithoutAuthorization() {
        AliceRequest request = buildRequest("привет");

        AliceResponse response = aliceService.handleWebhook(request, "Bearer valid-token");

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().isEnd_session()).isFalse();
        assertThat(response.getResponse().getText())
                .contains("Я навык приложения Chore Manager")
                .contains("что это за приложение")
                .contains("как приходят напоминания");

        verifyNoInteractions(hashService, accessTokenRepository, feignClient);
    }

    @Test
    void handleWebhook_helpCommand_returnsInstructionWithoutAuthorization() {
        AliceRequest request = buildRequest("помощь");

        AliceResponse response = aliceService.handleWebhook(request, null);

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .contains("Я навык приложения Chore Manager")
                .contains("что это за приложение")
                .contains("как приходят напоминания");

        verifyNoInteractions(hashService, accessTokenRepository, feignClient);
    }

    @Test
    void handleWebhook_applicationInfoCommand_returnsApplicationDescription() {
        AliceRequest request = buildRequest("что это за приложение");

        AliceResponse response = aliceService.handleWebhook(request, null);

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .contains("приложение для совместных бытовых задач")
                .contains("автоматически распределяет обязанности");

        verifyNoInteractions(hashService, accessTokenRepository, feignClient);
    }

    @Test
    void handleWebhook_distributionCommand_returnsDistributionDescription() {
        AliceRequest request = buildRequest("как работает распределение задач");

        AliceResponse response = aliceService.handleWebhook(request, null);

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText())
                .contains("Задачи назначаются автоматически")
                .contains("по кругу")
                .contains("по дням недели");

        verifyNoInteractions(hashService, accessTokenRepository, feignClient);
    }

    @Test
    void handleWebhook_unknownReferenceCommand_returnsReferenceFallback() {
        AliceRequest request = buildRequest("какой сегодня курс доллара");

        AliceResponse response = aliceService.handleWebhook(request, null);

        assertThat(response.getStart_account_linking()).isNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getText()).contains("Я не совсем поняла вопрос");

        verifyNoInteractions(hashService, accessTokenRepository, feignClient);
    }

    private AliceRequest buildRequest(String text) {
        AliceRequest.Request req = new AliceRequest.Request();
        req.setCommand(text);
        req.setOriginal_utterance(text);

        AliceRequest request = new AliceRequest();
        request.setRequest(req);
        request.setVersion("1.0");

        return request;
    }

    private AliceRequest buildRequestWithBodyToken(String text, String accessToken) {
        AliceRequest.Request req = new AliceRequest.Request();
        req.setCommand(text);
        req.setOriginal_utterance(text);

        AliceRequest.User user = new AliceRequest.User();
        user.setAccess_token(accessToken);

        AliceRequest.Session session = new AliceRequest.Session();
        session.setUser(user);

        AliceRequest request = new AliceRequest();
        request.setRequest(req);
        request.setSession(session);
        request.setVersion("1.0");

        return request;
    }

    private void mockValidToken(String rawToken, String tokenHash) {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Moscow"));
        when(clock.instant()).thenReturn(Instant.parse("2026-04-21T09:00:00Z"));

        AliceOAuthAccessToken validToken = AliceOAuthAccessToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.of(2026, 4, 21, 12, 0, 1))
                .build();

        when(hashService.sha256(rawToken)).thenReturn(tokenHash);
        when(accessTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(validToken));
    }
}
