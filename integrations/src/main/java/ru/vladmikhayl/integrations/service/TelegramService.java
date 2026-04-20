package ru.vladmikhayl.integrations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.vladmikhayl.integrations.dto.request.TelegramLinkRequest;
import ru.vladmikhayl.integrations.dto.request.TelegramSendMessageRequest;
import ru.vladmikhayl.integrations.dto.request.TelegramWebhookRequest;
import ru.vladmikhayl.integrations.feign.FeignClient;
import ru.vladmikhayl.integrations.feign.TelegramBotClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {
    private static final Pattern START_COMMAND_PATTERN =
            Pattern.compile("^/start(?:@\\w+)?(?:\\s+(?<token>.+))?$");

    private static final String SUCCESS_MESSAGE =
            "Аккаунт Telegram успешно привязан ✅";

    private static final String UNIVERSAL_MESSAGE =
            """
                    Этот бот умеет присылать напоминания о ваших бытовых задачах из сервиса Chore Manager ✨

                    Чтобы начать получать уведомления, перейдите в приложение и запустите подключение Telegram там""";

    private static final String DEFAULT_LINK_ERROR_MESSAGE =
            "Что-то пошло не так. Пожалуйста, попробуйте ещё раз";

    private final FeignClient feignClient;
    private final TelegramBotClient telegramBotClient;

    public void handleWebhook(TelegramWebhookRequest request) {
        log.info("Telegram webhook received");

        if (request == null || request.getMessage() == null) {
            return;
        }

        Long chatId = request.getMessage().getChat() != null
                ? request.getMessage().getChat().getId()
                : null;

        if (chatId == null) {
            return;
        }

        String text = request.getMessage().getText();

        if (text == null || text.isBlank()) {
            sendMessageSafely(chatId, UNIVERSAL_MESSAGE);
            return;
        }

        String token = extractStartToken(text);
        if (token == null) {
            sendMessageSafely(chatId, UNIVERSAL_MESSAGE);
            return;
        }

        TelegramLinkRequest linkRequest = new TelegramLinkRequest(token, chatId);

        try {
            feignClient.linkTelegramAccount(linkRequest);
            sendMessageSafely(chatId, SUCCESS_MESSAGE);
        } catch (Exception e) {
            sendMessageSafely(chatId, DEFAULT_LINK_ERROR_MESSAGE);
        }
    }

    private String extractStartToken(String text) {
        String trimmed = text.trim();
        Matcher matcher = START_COMMAND_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            return null;
        }

        String token = matcher.group("token");
        if (token == null || token.isBlank()) {
            return null;
        }

        return token.trim();
    }

    private void sendMessageSafely(Long chatId, String text) {
        try {
            telegramBotClient.sendMessage(new TelegramSendMessageRequest(chatId, text));
            log.info("Send message to chatId {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send telegram message to chatId={}", chatId, e);
        }
    }
}
