import { apiClient } from "./apiClient";
import type {
  AcceptInviteRequest,
  CreateInviteResponse,
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

export async function createInvite(
  listId: string,
): Promise<CreateInviteResponse> {
  const response = await apiClient.post<CreateInviteResponse>(
    `/lists/${listId}/invites`,
  );

  return response.data;
}

export async function acceptInvite(
  request: AcceptInviteRequest,
): Promise<void> {
  await apiClient.post("/invites/accept", request);
}
