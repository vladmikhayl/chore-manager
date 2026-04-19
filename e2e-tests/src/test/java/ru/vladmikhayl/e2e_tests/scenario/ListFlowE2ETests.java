package ru.vladmikhayl.e2e_tests.scenario;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import ru.vladmikhayl.e2e_tests.BaseE2ETest;
import ru.vladmikhayl.e2e_tests.dto.response.LoginResponse;
import ru.vladmikhayl.e2e_tests.dto.response.TodoListDetailsResponse;
import ru.vladmikhayl.e2e_tests.dto.response.TodoListMemberResponse;
import ru.vladmikhayl.e2e_tests.dto.response.TodoListShortResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ListFlowE2ETests extends BaseE2ETest {
    @Test
    void testCreateListAndViewItAsOwner() {
        // Создаём пользователя
        String login = generateRandomLogin();
        authHelper.register(login, DEFAULT_PASSWORD);
        LoginResponse loginResponse = authHelper.login(login, DEFAULT_PASSWORD);
        String token = loginResponse.getToken();

        // Создаём список
        listHelper.createList(token, DEFAULT_LIST_TITLE);

        // Проверяем, что список появился в общем списке
        List<TodoListShortResponse> lists = listHelper.getLists(token);
        assertThat(lists).isNotEmpty();

        TodoListShortResponse createdList = lists.stream()
                .filter(list -> list.getTitle().equals(DEFAULT_LIST_TITLE))
                .findFirst()
                .orElseThrow();

        assertThat(createdList.getId()).isNotNull();

        // Проверяем детали списка
        TodoListDetailsResponse details = listHelper.getListDetails(token, createdList.getId());

        assertThat(details).isNotNull();
        assertThat(details.getId()).isEqualTo(createdList.getId());
        assertThat(details.getTitle()).isEqualTo(DEFAULT_LIST_TITLE);
        assertThat(details.getMembers()).hasSize(1);
        assertThat(details.getMembers().stream().map(TodoListMemberResponse::getLogin))
                .contains(login);
    }

    @Test
    void testCreateInviteAndAcceptItSuccessfully() {
        // Создаём владельца списка
        String ownerLogin = generateRandomLogin();
        authHelper.register(ownerLogin, DEFAULT_PASSWORD);
        String ownerToken = authHelper.login(ownerLogin, DEFAULT_PASSWORD).getToken();

        // Создаём второго пользователя
        String invitedLogin = generateRandomLogin();
        authHelper.register(invitedLogin, DEFAULT_PASSWORD);
        String invitedToken = authHelper.login(invitedLogin, DEFAULT_PASSWORD).getToken();

        // Владелец создаёт список
        listHelper.createList(ownerToken, DEFAULT_LIST_TITLE);

        TodoListShortResponse createdList = listHelper.getLists(ownerToken).stream()
                .filter(list -> list.getTitle().equals(DEFAULT_LIST_TITLE))
                .findFirst()
                .orElseThrow();

        UUID listId = createdList.getId();

        // Владелец создаёт приглашение
        String inviteToken = listHelper.createInvite(ownerToken, listId).getToken();
        assertThat(inviteToken).isNotBlank();

        // Второй пользователь принимает приглашение
        listHelper.acceptInvite(invitedToken, inviteToken);

        // Проверяем, что у второго пользователя появился список
        List<TodoListShortResponse> invitedUserLists = listHelper.getLists(invitedToken);

        TodoListShortResponse joinedList = invitedUserLists.stream()
                .filter(list -> list.getId().equals(listId))
                .findFirst()
                .orElseThrow();

        assertThat(joinedList.getTitle()).isEqualTo(DEFAULT_LIST_TITLE);

        // Проверяем, что в деталях списка теперь два участника
        TodoListDetailsResponse details = listHelper.getListDetails(ownerToken, listId);

        assertThat(details.getMembers()).hasSize(2);
        assertThat(details.getMembers().stream().map(TodoListMemberResponse::getLogin))
                .contains(ownerLogin, invitedLogin);
    }

    @Test
    void testAcceptInvalidInviteReturnsNotFound() {
        // Создаём пользователя
        String login = generateRandomLogin();
        authHelper.register(login, DEFAULT_PASSWORD);
        String token = authHelper.login(login, DEFAULT_PASSWORD).getToken();

        String invalidInviteToken = "invalid-invite-token";

        // Пытаемся принять невалидный invite
        assertThatThrownBy(() -> listHelper.acceptInvite(token, invalidInviteToken))
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        // Проверяем, что никакие списки не появились
        List<TodoListShortResponse> lists = listHelper.getLists(token);
        assertThat(lists).isEmpty();
    }
}
