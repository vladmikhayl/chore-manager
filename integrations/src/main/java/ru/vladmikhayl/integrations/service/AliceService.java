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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliceService {
    private static final List<Pattern> COMPLETE_TASK_PATTERNS = List.of(
            Pattern.compile(
                    "^отмет\\p{L}*\\s+задач\\p{L}*\\s+выполнен\\p{L}*\\s+(.+)$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            ),
            Pattern.compile(
                    "^отмет\\p{L}*\\s+выполнен\\p{L}*\\s+(?:задач\\p{L}*\\s+)?(.+)$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            ),
            Pattern.compile(
                    "^отмет\\p{L}*\\s+(?:задач\\p{L}*\\s+)?(.+?)\\s+выполнен\\p{L}*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            ),
            Pattern.compile(
                    "^(?:задач\\p{L}*\\s+)?(.+?)\\s+отмет\\p{L}*(?:\\s+выполнен\\p{L}*)?$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            ),
            Pattern.compile(
                    "^отмет\\p{L}*\\s+(?:задач\\p{L}*\\s+)?(.+)$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            )
    );

    private static final String ACCOUNT_LINKING_SUCCESS_TEXT =
            "Вы успешно авторизовались. Теперь вы можете спросить, какие у вас задачи на сегодня или на завтра, или попросить отметить задачу выполненной.";

    private static final String START_HELP_TEXT = """
        Я навык приложения Chore Manager. Через меня вы можете взаимодействовать с сервисом голосом: например, спросить свои задачи на сегодня или на завтра, а также отметить задачу выполненной.
        А еще я могу рассказать о самом сервисе: например, что это за приложение, как распределяются задачи или как приходят напоминания.
        """;

    private static final String APP_INFO_TEXT =
            "Это приложение для совместных бытовых задач. Оно помогает не только вести список дел, но и автоматически распределяет обязанности между участниками.";

    private static final String DISTRIBUTION_INFO_TEXT =
            "Задачи назначаются автоматически. Можно закрепить задачу за человеком, распределять по кругу или задать исполнителей по дням недели.";

    private static final String REMINDERS_INFO_TEXT =
            "Приложение отправляет напоминания через Телеграм. Пользователь получает список задач на день в 8 утра каждый день.";

    private static final String UNKNOWN_COMMAND_TEXT =
            "Я не совсем поняла вопрос. Попробуйте спросить, какие у меня задачи на сегодня или на завтра, попросите отметить задачу выполненной, или спросите, что делает приложение.";

    private static final String COMPLETE_TASK_FORMAT_TEXT =
            "Чтобы отметить задачу выполненной, скажите, например: отметь задачу \"вынести мусор\".";

    private static final String DUPLICATE_TASKS_TEXT =
            "На сегодня нашлось несколько задач с таким названием. К сожалению, пока я не могу отмечать задачи в таком случае.";

    private static final List<String> TASK_NOT_FOUND_TODAY_TEXTS = List.of(
            "Кажется, на сегодня у вас нет такой задачи.",
            "Не нашла такую задачу на сегодня.",
            "Похоже, сегодня такой задачи нет."
    );

    private static final List<String> COMPLETE_TASK_SUCCESS_TEXTS = List.of(
            "Готово! Задача отмечена выполненной.",
            "Отметила выполненной. Так держать!",
            "Отметила. Хорошая работа!",
            "Готово, отметила."
    );

    private final FeignClient feignClient;
    private final HashService hashService;
    private final AliceOAuthAccessTokenRepository accessTokenRepository;
    private final Clock clock;

    public AliceResponse handleWebhook(AliceRequest request, String authorizationHeader) {
        String version = request != null && request.getVersion() != null
                ? request.getVersion()
                : "1.0";

        if (request != null && request.getAccount_linking_complete_event() != null) {
            UUID userId = resolveUserId(request, authorizationHeader);

            if (userId == null) {
                return buildStartAccountLinkingResponse(version);
            }

            return new AliceResponse(
                    new AliceResponse.Response(ACCOUNT_LINKING_SUCCESS_TEXT, false),
                    null,
                    version
            );
        }

        String command = extractCommand(request);

        if (isCompleteTaskCommand(command)) {
            UUID userId = resolveUserId(request, authorizationHeader);

            if (userId == null) {
                return buildStartAccountLinkingResponse(version);
            }

            return new AliceResponse(
                    new AliceResponse.Response(completeTask(userId, command), false),
                    null,
                    version
            );
        }

        if (isTodayCommand(command) || isTomorrowCommand(command)) {
            UUID userId = resolveUserId(request, authorizationHeader);

            if (userId == null) {
                return buildStartAccountLinkingResponse(version);
            }

            LocalDate date = isTodayCommand(command)
                    ? LocalDate.now(clock)
                    : LocalDate.now(clock).plusDays(1);

            return new AliceResponse(
                    new AliceResponse.Response(buildTasksMessage(userId, date), false),
                    null,
                    version
            );
        }

        return new AliceResponse(
                new AliceResponse.Response(buildReferenceAnswer(command), false),
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

    private boolean isCompleteTaskCommand(String command) {
        return containsAny(command, "отмет", "выполн");
    }

    private boolean isTodayCommand(String command) {
        return command.contains("сегодня") && command.contains("задач");
    }

    private boolean isTomorrowCommand(String command) {
        return command.contains("завтра") && command.contains("задач");
    }

    private String completeTask(UUID userId, String command) {
        Optional<String> requestedTaskTitleOptional = extractTaskTitleToComplete(command);

        if (requestedTaskTitleOptional.isEmpty()) {
            return COMPLETE_TASK_FORMAT_TEXT;
        }

        String requestedTaskTitle = requestedTaskTitleOptional.get();

        LocalDate today = LocalDate.now(clock);
        List<TaskResponseShort> todayTasks = feignClient.getTasksForDay(userId, today.toString());

        if (todayTasks == null || todayTasks.isEmpty()) {
            return "На сегодня у вас нет задач.";
        }

        List<TaskResponseShort> matchedTasks = todayTasks.stream()
                .filter(task -> isSameTaskTitle(task.getTitle(), requestedTaskTitle))
                .toList();

        if (matchedTasks.isEmpty()) {
            return randomText(TASK_NOT_FOUND_TODAY_TEXTS);
        }

        if (matchedTasks.size() > 1) {
            return DUPLICATE_TASKS_TEXT;
        }

        TaskResponseShort task = matchedTasks.get(0);

        feignClient.completeTask(userId, task.getId(), today.toString());

        return randomText(COMPLETE_TASK_SUCCESS_TEXTS);
    }

    private Optional<String> extractTaskTitleToComplete(String command) {
        for (Pattern pattern : COMPLETE_TASK_PATTERNS) {
            Matcher matcher = pattern.matcher(command);

            if (matcher.matches()) {
                String title = matcher.group(1).trim();
                return title.isBlank()
                        ? Optional.empty()
                        : Optional.of(title);
            }
        }

        return Optional.empty();
    }

    private boolean isSameTaskTitle(String actualTitle, String requestedTitle) {
        return normalizeTaskTitle(actualTitle).equals(normalizeTaskTitle(requestedTitle));
    }

    private String normalizeTaskTitle(String title) {
        return Objects.requireNonNullElse(title, "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
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

        sb.append(getTaskCountText(tasks.size())).append(": ");

        for (int i = 0; i < tasks.size(); i++) {
            TaskResponseShort task = tasks.get(i);
            sb.append(task.getTitle().toLowerCase(Locale.ROOT));

            if (i < tasks.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(".");

        return sb.toString();
    }

    private String getTaskCountText(int count) {
        return switch (count) {
            case 1 -> "одна задача";
            case 2 -> "две задачи";
            case 3 -> "три задачи";
            default -> "несколько задач";
        };
    }

    private String buildReferenceAnswer(String command) {
        if (command == null || command.isBlank() || containsAny(command, "привет", "запусти", "начать", "старт", "помощ", "что ты умеешь")) {
            return START_HELP_TEXT;
        }

        if (containsAny(command, "что делает", "о приложении", "что это", "что за приложение", "что умеет")) {
            return APP_INFO_TEXT;
        }

        if (containsAny(command, "распредел", "как назначаются", "кто выполняет")) {
            return DISTRIBUTION_INFO_TEXT;
        }

        if (containsAny(command, "напоминан", "telegram", "телеграм", "уведомлен")) {
            return REMINDERS_INFO_TEXT;
        }

        return UNKNOWN_COMMAND_TEXT;
    }

    private AliceResponse buildStartAccountLinkingResponse(String version) {
        return new AliceResponse(
                null,
                Map.of(),
                version
        );
    }

    private boolean containsAny(String command, String... parts) {
        for (String part : parts) {
            if (command.contains(part)) {
                return true;
            }
        }

        return false;
    }

    private String randomText(List<String> texts) {
        return texts.get(new Random().nextInt(texts.size()));
    }
}
