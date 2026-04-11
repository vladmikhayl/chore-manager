package ru.vladmikhayl.reminders.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.vladmikhayl.reminders.dto.request.TasksForUsersRequest;
import ru.vladmikhayl.reminders.dto.response.ReminderTaskResponse;
import ru.vladmikhayl.reminders.dto.response.UserTasksForReminderResponse;
import ru.vladmikhayl.reminders.entity.ReminderUserSettings;
import ru.vladmikhayl.reminders.feign.TaskManagementClient;
import ru.vladmikhayl.reminders.repository.ReminderUserSettingsRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderDispatchService {
    private final ReminderUserSettingsRepository reminderUserSettingsRepository;
    private final TaskManagementClient taskManagementClient;
    private final TelegramClient telegramClient;

    public void sendDailyReminders() {
        List<ReminderUserSettings> users =
                reminderUserSettingsRepository.findAllByChatIdIsNotNullAndDailyReminderEnabledTrue();

        if (users.isEmpty()) {
            log.info("Нет пользователей для отправки ежедневных напоминаний");
            return;
        }

        List<UUID> userIds = users.stream()
                .map(ReminderUserSettings::getUserId)
                .toList();

        LocalDate today = LocalDate.now();

        List<UserTasksForReminderResponse> tasksByUser = taskManagementClient.getTasksForUsers(
                TasksForUsersRequest.builder()
                        .userIds(userIds)
                        .date(today)
                        .build()
        );

        Map<UUID, Long> chatIdByUserId = users.stream()
                .collect(Collectors.toMap(ReminderUserSettings::getUserId, ReminderUserSettings::getChatId));

        for (UserTasksForReminderResponse userTasks : tasksByUser) {
            log.info("Обработка пользователя userId={}", userTasks.getUserId());

            List<ReminderTaskResponse> tasks = userTasks.getTasks();

            if (tasks == null) {
                continue;
            }

            Long chatId = chatIdByUserId.get(userTasks.getUserId());
            if (chatId == null) {
                continue;
            }

            String message = buildMessage(tasks);
            if (message == null || message.isBlank()) {
                continue;
            }

            try {
                telegramClient.sendMessage(chatId, message);
                log.info("Напоминание отправлено userId={}, chatId={}", userTasks.getUserId(), chatId);
            } catch (Exception e) {
                log.error("Не удалось отправить напоминание userId={}, chatId={}",
                        userTasks.getUserId(), chatId, e);
            }
        }
    }

    private String buildMessage(List<ReminderTaskResponse> tasks) {
        StringBuilder sb = new StringBuilder();

        sb.append("\uD83D\uDCCC <b>Задачи на сегодня</b>\n\n");

        if (tasks == null || tasks.isEmpty()) {
            sb.append("Сегодня у вас нет задач 🎉");
            return sb.toString();
        }

        for (int i = 0; i < tasks.size(); i++) {
            ReminderTaskResponse task = tasks.get(i);

            sb.append(i + 1)
                    .append(") <b>")
                    .append(task.getTitle())
                    .append("</b>")
                    .append(" — ")
                    .append(task.getListTitle())
                    .append("\n");
        }

        return sb.toString();
    }
}
