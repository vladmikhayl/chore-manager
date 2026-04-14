package ru.vladmikhayl.integrations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vladmikhayl.integrations.dto.request.AliceRequest;
import ru.vladmikhayl.integrations.dto.response.AliceResponse;
import ru.vladmikhayl.integrations.dto.response.TaskResponseShort;
import ru.vladmikhayl.integrations.feign.FeignClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AliceService {
    private static final UUID TEST_USER_ID =
            UUID.fromString("8f0118cc-029b-4d69-a39a-44e2ae956f8d");

    private final FeignClient feignClient;

    public AliceResponse handleWebhook(AliceRequest request) {
        return new AliceResponse(
                null,
                Map.of(),
                request != null && request.getVersion() != null ? request.getVersion() : "1.0"
        );

//        String command = extractCommand(request);
//
//        String text;
//
//        if (isTodayCommand(command)) {
//            text = buildTasksMessage(LocalDate.now());
//        } else if (isTomorrowCommand(command)) {
//            text = buildTasksMessage(LocalDate.now().plusDays(1));
//        } else {
//            text = "К сожалению, распознать команду не удалось. Пока я умею только рассказывать о задачах на сегодня и завтра.";
//        }
//
//        return new AliceResponse(
//                new AliceResponse.Response(text, false),
//                request != null && request.getVersion() != null ? request.getVersion() : "1.0"
//        );
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

    private String buildTasksMessage(LocalDate date) {
        List<TaskResponseShort> tasks = feignClient.getTasksForDay(TEST_USER_ID, date.toString());

        if (tasks == null || tasks.isEmpty()) {
            return date.equals(LocalDate.now())
                    ? "На сегодня у вас нет задач."
                    : "На завтра у вас нет задач.";
        }

        StringBuilder sb = new StringBuilder();

        if (date.equals(LocalDate.now())) {
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
