package ru.vladmikhayl.task_management;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.*;

@TestConfiguration
public class TestTimeConfig {
    @Bean
    public Clock clock() {
        ZoneId zone = ZoneId.of("Europe/Moscow");
        return Clock.fixed(
                LocalDateTime.of(2026, 3, 8, 0, 0)
                        .atZone(zone)
                        .toInstant(),
                zone
        );
    }
}
