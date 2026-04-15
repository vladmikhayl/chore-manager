package ru.vladmikhayl.integrations.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "alice.oauth")
public class AliceOAuthProperties {
    private String clientId;
    private String clientSecret;
    private int authorizationCodeLifetimeSeconds = 60;
    private int accessTokenLifetimeSeconds = 2_592_000; // 30 дней
}
