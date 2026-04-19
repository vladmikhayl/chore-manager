package ru.vladmikhayl.e2e_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.web.client.RestTemplate;
import ru.vladmikhayl.e2e_tests.helper.AuthHelper;

import java.time.LocalDate;
import java.util.UUID;

public abstract class BaseE2ETest {
    protected static final String TODAY_DATE_STR = LocalDate.now().toString();

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("../")
            .filename(".env")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    protected final String gatewayUrl = dotenv.get("E2E_API_BASE_URL");

    protected final RestTemplate restTemplate = new RestTemplate();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final AuthHelper authHelper = new AuthHelper(restTemplate, gatewayUrl, objectMapper);

    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    protected String generateRandomLogin() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}