package ru.vladmikhayl.integrations.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladmikhayl.integrations.dto.request.AliceRequest;
import ru.vladmikhayl.integrations.dto.response.AliceResponse;
import ru.vladmikhayl.integrations.dto.response.TaskResponseShort;
import ru.vladmikhayl.integrations.feign.FeignClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AliceServiceTest {
    @Mock
    private FeignClient feignClient;

    @InjectMocks
    private AliceService aliceService;

    @Test
    void handleWebhook_requestIsNull_returnsFallback() {
        AliceResponse response = aliceService.handleWebhook(null);

        assertThat(response.getResponse().getText())
                .startsWith("К сожалению, распознать команду не удалось");

        verifyNoInteractions(feignClient);
    }

    @Test
    void handleWebhook_unknownCommand_returnsFallback() {
        AliceRequest request = buildRequest("привет");

        AliceResponse response = aliceService.handleWebhook(request);

        assertThat(response.getResponse().getText())
                .startsWith("К сожалению, распознать команду не удалось");

        verifyNoInteractions(feignClient);
    }

    @Test
    void handleWebhook_todayCommand_returnsTasks() {
        when(feignClient.getTasksForDay(any(), any()))
                .thenReturn(List.of(
                        TaskResponseShort.builder().title("Вынести мусор").build(),
                        TaskResponseShort.builder().title("Купить продукты").build()
                ));

        AliceRequest request = buildRequest("какие у меня задачи на сегодня");

        AliceResponse response = aliceService.handleWebhook(request);

        assertThat(response.getResponse().getText())
                .contains("На сегодня у вас 2 задачи")
                .contains("Вынести мусор")
                .contains("Купить продукты");

        verify(feignClient).getTasksForDay(any(), eq(LocalDate.now().toString()));
    }

    @Test
    void handleWebhook_tomorrowCommand_returnsTasks() {
        when(feignClient.getTasksForDay(any(), any()))
                .thenReturn(List.of(
                        TaskResponseShort.builder().title("Помыть посуду").build()
                ));

        AliceRequest request = buildRequest("какие задачи на завтра");

        AliceResponse response = aliceService.handleWebhook(request);

        assertThat(response.getResponse().getText())
                .contains("На завтра у вас 1 задача")
                .contains("Помыть посуду");

        verify(feignClient).getTasksForDay(any(), eq(LocalDate.now().plusDays(1).toString()));
    }

    @Test
    void handleWebhook_noTasks_returnsEmptyMessage() {
        when(feignClient.getTasksForDay(any(), any()))
                .thenReturn(List.of());

        AliceRequest request = buildRequest("задачи на сегодня");

        AliceResponse response = aliceService.handleWebhook(request);

        assertThat(response.getResponse().getText())
                .isEqualTo("На сегодня у вас нет задач.");
    }

    private AliceRequest buildRequest(String text) {
        AliceRequest.Request req = new AliceRequest.Request();
        req.setCommand(text);
        req.setOriginal_utterance(text);

        AliceRequest request = new AliceRequest();
        request.setRequest(req);
        request.setVersion("1.0");

        return request;
    }
}
