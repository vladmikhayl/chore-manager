package ru.vladmikhayl.reminders.config;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.vladmikhayl.reminders.dto.kafka.ReminderSettingsChangedEvent;
import ru.vladmikhayl.reminders.dto.kafka.TelegramLinkedEvent;
import ru.vladmikhayl.reminders.dto.kafka.TelegramUnlinkedEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TelegramLinkedEvent> telegramLinkedKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        return buildFactory(kafkaProperties, TelegramLinkedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TelegramUnlinkedEvent> telegramUnlinkedKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        return buildFactory(kafkaProperties, TelegramUnlinkedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReminderSettingsChangedEvent> reminderSettingsChangedKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        return buildFactory(kafkaProperties, ReminderSettingsChangedEvent.class);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> buildFactory(
            KafkaProperties kafkaProperties,
            Class<T> clazz
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(clazz);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeHeaders(false);

        DefaultKafkaConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        valueDeserializer
                );

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        return factory;
    }
}
