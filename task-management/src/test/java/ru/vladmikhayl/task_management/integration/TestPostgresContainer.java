package ru.vladmikhayl.task_management.integration;

import org.testcontainers.containers.PostgreSQLContainer;

// Это класс, в котором в виде синглтона хранится контейнер Postgres для интеграционных тестов.
// Это сделано, чтобы при одновременном запуске сразу нескольких классов с интеграционными тестами они работали
// с одним контейнером Postgres.
public class TestPostgresContainer extends PostgreSQLContainer<TestPostgresContainer> {
    private static final String IMAGE_VERSION = "postgres:16-alpine";

    private static TestPostgresContainer container;

    private TestPostgresContainer() {
        super(IMAGE_VERSION);
    }

    public static synchronized TestPostgresContainer getInstance() {
        if (container == null) {
            container = new TestPostgresContainer()
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
            container.start();
        }
        return container;
    }

    @Override
    public void stop() {
        // Не останавливаем контейнер, чтобы он продолжал жить на все время тестов.
        // Он все равно автоматически остановится при завершении JVM (то есть после завершения всех запущенных классов с тестами).
    }
}
