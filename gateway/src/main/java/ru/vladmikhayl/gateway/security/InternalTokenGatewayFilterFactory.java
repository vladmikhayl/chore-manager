package ru.vladmikhayl.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component("InternalToken")
public class InternalTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<InternalTokenGatewayFilterFactory.Config> {
    private static final String HEADER = "X-Internal-Token";

    @Value("${security.internal.token}")
    private String expectedToken;

    public InternalTokenGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = exchange.getRequest().getHeaders().getFirst(HEADER);

            if (token == null || token.isBlank() || !token.equals(expectedToken)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // пока не нужны параметры
    }
}
