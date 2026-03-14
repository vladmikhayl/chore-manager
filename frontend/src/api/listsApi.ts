import { apiClient } from "./apiClient";
import type { TodoListShortResponse } from "../types/lists";

export async function getLists(): Promise<TodoListShortResponse[]> {
  const response = await apiClient.get<TodoListShortResponse[]>("/lists");
  return response.data;
}
