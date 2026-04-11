package ru.vladmikhayl.identity;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import ru.vladmikhayl.identity.service.IdentityEventPublisher;

import java.util.UUID;

@TestConfiguration
public class EventPublisherTestConfig {
    @Bean
    IdentityEventPublisher identityEventPublisher() {
        return new IdentityEventPublisher(null) {
            @Override
            public void publishTelegramLinked(UUID userId, Long chatId) {
            }

            @Override
            public void publishTelegramUnlinked(UUID userId) {
            }

            @Override
            public void publishReminderSettingsChanged(UUID userId, boolean dailyReminderEnabled) {
            }
        };
    }
}
