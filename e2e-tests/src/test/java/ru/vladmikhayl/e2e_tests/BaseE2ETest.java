package ru.vladmikhayl.e2e_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.web.client.RestTemplate;
import ru.vladmikhayl.e2e_tests.helper.AuthHelper;
import ru.vladmikhayl.e2e_tests.helper.ListHelper;
import ru.vladmikhayl.e2e_tests.helper.TaskHelper;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public abstract class BaseE2ETest {
    protected static final String DEFAULT_PASSWORD = "12345";
    protected static final String DEFAULT_LIST_TITLE = "Список домашних дел";
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
    protected final ListHelper listHelper = new ListHelper(restTemplate, gatewayUrl, objectMapper);
    protected final TaskHelper taskHelper = new TaskHelper(restTemplate, gatewayUrl, objectMapper);

    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    protected String generateRandomLogin() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    protected String generateRandomTaskTitle() {
        return "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    protected int toApiWeekday(LocalDate date) {
        return date.getDayOfWeek().getValue() - 1; // Monday=0 ... Sunday=6
    }

    protected Map<Integer, UUID> createWeekdayMap(UUID defaultUserId) {
        return new java.util.HashMap<>(Map.of(
                0, defaultUserId,
                1, defaultUserId,
                2, defaultUserId,
                3, defaultUserId,
                4, defaultUserId,
                5, defaultUserId,
                6, defaultUserId
        ));
    }
}