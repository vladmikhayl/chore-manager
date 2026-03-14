import { apiClient } from "./apiClient";
import type { TaskResponse } from "../types/tasks";

export async function getTasksForDay(date: string): Promise<TaskResponse[]> {
  const response = await apiClient.get<TaskResponse[]>("/tasks", {
    params: { date },
  });

  return response.data;
}
