package ru.vladmikhayl.integrations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.vladmikhayl.integrations.dto.request.AliceRequest;
import ru.vladmikhayl.integrations.dto.response.AliceResponse;
import ru.vladmikhayl.integrations.dto.response.TaskResponseShort;
import ru.vladmikhayl.integrations.entity.AliceOAuthAccessToken;
import ru.vladmikhayl.integrations.feign.FeignClient;
import ru.vladmikhayl.integrations.repository.AliceOAuthAccessTokenRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliceService {
    private final FeignClient feignClient;
    private final HashService hashService;
    private final AliceOAuthAccessTokenRepository accessTokenRepository;
    private final Clock clock;

    public AliceResponse handleWebhook(AliceRequest request, String authorizationHeader) {
        String version = request != null && request.getVersion() != null
                ? request.getVersion()
                : "1.0";

        UUID userId = resolveUserId(request, authorizationHeader);

        if (userId == null) {
            return new AliceResponse(
                    null,
                    Map.of(),
                    version
            );
        }

        String command = extractCommand(request);
        String text;

        if (isTodayCommand(command)) {
            text = buildTasksMessage(userId, LocalDate.now(clock));
        } else if (isTomorrowCommand(command)) {
            text = buildTasksMessage(userId, LocalDate.now(clock).plusDays(1));
        } else {
            text = "К сожалению, распознать команду не удалось. Пока я умею только рассказывать о задачах на сегодня и завтра.";
        }

        return new AliceResponse(
                new AliceResponse.Response(text, false),
                null,
                version
        );
    }

    private UUID resolveUserId(AliceRequest request, String authorizationHeader) {
        String token = extractAccessToken(request, authorizationHeader);

        if (token == null) {
            return null;
        }

        String tokenHash = hashService.sha256(token);

        return accessTokenRepository.findByTokenHash(tokenHash)
                .filter(accessToken -> !accessToken.getExpiresAt().isBefore(LocalDateTime.now(clock)))
                .map(AliceOAuthAccessToken::getUserId)
                .orElse(null);
    }

    private String extractAccessToken(AliceRequest request, String authorizationHeader) {
        String headerToken = extractTokenFromAuthorizationHeader(authorizationHeader);
        if (headerToken != null) {
            return headerToken;
        }

        return extractTokenFromRequestBody(request);
    }

    private String extractTokenFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        String trimmed = authorizationHeader.trim();

        if (trimmed.regionMatches(true, 0, "OAuth ", 0, 6)) {
            String token = trimmed.substring(6).trim();
            return token.isBlank() ? null : token;
        }

        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = trimmed.substring(7).trim();
            return token.isBlank() ? null : token;
        }

        return null;
    }

    private String extractTokenFromRequestBody(AliceRequest request) {
        if (request == null
                || request.getSession() == null
                || request.getSession().getUser() == null) {
            return null;
        }

        String token = request.getSession().getUser().getAccess_token();

        return token == null || token.isBlank()
                ? null
                : token.trim();
    }

    private String extractCommand(AliceRequest request) {
        if (request == null || request.getRequest() == null) {
            return "";
        }

        String command = request.getRequest().getCommand();
        if (command == null || command.isBlank()) {
            command = request.getRequest().getOriginal_utterance();
        }

        return command == null ? "" : command.toLowerCase(Locale.ROOT).trim();
    }

    private boolean isTodayCommand(String command) {
        return command.contains("сегодня") && command.contains("задач");
    }

    private boolean isTomorrowCommand(String command) {
        return command.contains("завтра") && command.contains("задач");
    }

    private String buildTasksMessage(UUID userId, LocalDate date) {
        List<TaskResponseShort> tasks = feignClient.getTasksForDay(userId, date.toString());

        if (tasks == null || tasks.isEmpty()) {
            return date.equals(LocalDate.now(clock))
                    ? "На сегодня у вас нет задач."
                    : "На завтра у вас нет задач.";
        }

        StringBuilder sb = new StringBuilder();

        if (date.equals(LocalDate.now(clock))) {
            sb.append("На сегодня у вас ");
        } else {
            sb.append("На завтра у вас ");
        }

        sb.append(tasks.size()).append(getTaskWord(tasks.size())).append(": ");

        for (int i = 0; i < tasks.size(); i++) {
            TaskResponseShort task = tasks.get(i);
            sb.append(task.getTitle());

            if (i < tasks.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(".");

        return sb.toString();
    }

    private String getTaskWord(int count) {
        int mod100 = count % 100;
        int mod10 = count % 10;

        if (mod100 >= 11 && mod100 <= 14) {
            return " задач";
        }
        if (mod10 == 1) {
            return " задача";
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return " задачи";
        }
        return " задач";
    }
}
