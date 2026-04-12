package ru.vladmikhayl.integrations.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladmikhayl.integrations.dto.request.TelegramLinkRequest;
import ru.vladmikhayl.integrations.dto.request.TelegramSendMessageRequest;
import ru.vladmikhayl.integrations.dto.request.TelegramWebhookRequest;
import ru.vladmikhayl.integrations.feign.FeignClient;
import ru.vladmikhayl.integrations.feign.TelegramBotClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IntegrationsServiceTest {
    private static final Long CHAT_ID = 123456789L;

    @Mock
    private FeignClient feignClient;

    @Mock
    private TelegramBotClient telegramBotClient;

    @InjectMocks
    private TelegramService telegramService;

    @Test
    void handleWebhook_requestIsNull_doesNothing() {
        telegramService.handleWebhook(null);
        verifyNoInteractions(feignClient, telegramBotClient);
    }

    @Test
    void handleWebhook_messageIsNull_doesNothing() {
        TelegramWebhookRequest request = new TelegramWebhookRequest();
        request.setMessage(null);
        telegramService.handleWebhook(request);
        verifyNoInteractions(feignClient, telegramBotClient);
    }

    @Test
    void handleWebhook_chatIsNull_doesNothing() {
        TelegramWebhookRequest.TelegramMessage message = new TelegramWebhookRequest.TelegramMessage();
        message.setText("/start token");
        message.setChat(null);
        TelegramWebhookRequest request = new TelegramWebhookRequest();
        request.setMessage(message);
        telegramService.handleWebhook(request);
        verifyNoInteractions(feignClient, telegramBotClient);
    }

    @Test
    void handleWebhook_chatIdIsNull_doesNothing() {
        TelegramWebhookRequest.TelegramChat chat = new TelegramWebhookRequest.TelegramChat();
        chat.setId(null);
        TelegramWebhookRequest.TelegramMessage message = new TelegramWebhookRequest.TelegramMessage();
        message.setText("/start token");
        message.setChat(chat);
        TelegramWebhookRequest request = new TelegramWebhookRequest();
        request.setMessage(message);
        telegramService.handleWebhook(request);
        verifyNoInteractions(feignClient, telegramBotClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Привет", "/start", "/start    "})
    void handleWebhook_invalidText_sendsUniversalMessage(String text) {
        TelegramWebhookRequest request = buildRequest(text, CHAT_ID);

        telegramService.handleWebhook(request);

        verifyNoInteractions(feignClient);

        ArgumentCaptor<TelegramSendMessageRequest> captor =
                ArgumentCaptor.forClass(TelegramSendMessageRequest.class);
        verify(telegramBotClient).sendMessage(captor.capture());

        TelegramSendMessageRequest sentMessage = captor.getValue();
        assertThat(sentMessage.getChat_id()).isEqualTo(CHAT_ID);
        assertThat(sentMessage.getText()).startsWith("Этот бот умеет");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/start abc123",
            "/start@ChoreManagerBot abc123"
    })
    void handleWebhook_validStartCommand_linksTelegramAndSendsSuccessMessage(String text) {
        TelegramWebhookRequest request = buildRequest(text, CHAT_ID);

        telegramService.handleWebhook(request);

        verify(feignClient).linkTelegramAccount(new TelegramLinkRequest("abc123", CHAT_ID));

        ArgumentCaptor<TelegramSendMessageRequest> captor =
                ArgumentCaptor.forClass(TelegramSendMessageRequest.class);
        verify(telegramBotClient).sendMessage(captor.capture());

        TelegramSendMessageRequest sentMessage = captor.getValue();
        assertThat(sentMessage.getChat_id()).isEqualTo(CHAT_ID);
        assertThat(sentMessage.getText()).startsWith("Аккаунт Telegram успешно привязан");
    }

    @Test
    void handleWebhook_identityClientThrowsException_sendsDefaultErrorMessage() {
        TelegramWebhookRequest request = buildRequest("/start abc123", CHAT_ID);

        doThrow(new RuntimeException("boom"))
                .when(feignClient)
                .linkTelegramAccount(new TelegramLinkRequest("abc123", CHAT_ID));

        telegramService.handleWebhook(request);

        ArgumentCaptor<TelegramSendMessageRequest> captor =
                ArgumentCaptor.forClass(TelegramSendMessageRequest.class);
        verify(telegramBotClient).sendMessage(captor.capture());

        TelegramSendMessageRequest sentMessage = captor.getValue();
        assertThat(sentMessage.getChat_id()).isEqualTo(CHAT_ID);
        assertThat(sentMessage.getText()).startsWith("Что-то пошло не так");
    }

    private TelegramWebhookRequest buildRequest(String text, Long chatId) {
        TelegramWebhookRequest.TelegramChat chat = new TelegramWebhookRequest.TelegramChat();
        chat.setId(chatId);

        TelegramWebhookRequest.TelegramMessage message = new TelegramWebhookRequest.TelegramMessage();
        message.setText(text);
        message.setChat(chat);

        TelegramWebhookRequest request = new TelegramWebhookRequest();
        request.setMessage(message);

        return request;
    }
}
