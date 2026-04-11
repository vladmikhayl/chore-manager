package ru.vladmikhayl.reminders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.vladmikhayl.reminders.service.ReminderDispatchService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {
    private final ReminderDispatchService reminderDispatchService;

    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Moscow")
    public void runDailyReminders() {
        log.info("Запуск ежедневной рассылки напоминаний");
        reminderDispatchService.sendDailyReminders();
    }
}
