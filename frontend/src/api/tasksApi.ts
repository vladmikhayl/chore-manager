import { apiClient } from "./apiClient";
import type { TaskResponse } from "../types/tasks";

export async function getTasksForDay(date: string): Promise<TaskResponse[]> {
  const response = await apiClient.get<TaskResponse[]>("/tasks", {
    params: { date },
  });

  return response.data;
}

export async function completeTask(
  taskId: string,
  date: string,
): Promise<void> {
  await apiClient.put(`/tasks/${taskId}/completions/${date}`);
}

export async function deleteTaskCompletion(
  taskId: string,
  date: string,
): Promise<void> {
  await apiClient.delete(`/tasks/${taskId}/completions/${date}`);
}
