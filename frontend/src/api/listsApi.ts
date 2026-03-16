import { apiClient } from "./apiClient";
import type {
  CreateTodoListRequest,
  TodoListDetailsResponse,
  TodoListShortResponse,
} from "../types/lists";

export async function getLists(): Promise<TodoListShortResponse[]> {
  const response = await apiClient.get<TodoListShortResponse[]>("/lists");
  return response.data;
}

export async function createList(
  request: CreateTodoListRequest,
): Promise<void> {
  await apiClient.post("/lists", request);
}

export async function getListDetails(
  listId: string,
): Promise<TodoListDetailsResponse> {
  const response = await apiClient.get<TodoListDetailsResponse>(
    `/lists/${listId}`,
  );
  return response.data;
}
