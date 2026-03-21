import { apiClient } from "./apiClient";
import type { CreateTaskRequest, TaskResponse } from "../types/tasks";

export async function getTasksForDay(date: string): Promise<TaskResponse[]> {
  const response = await apiClient.get<TaskResponse[]>("/tasks", {
    params: { date },
  });

  return response.data;
}

export async function getListTasks(listId: string): Promise<TaskResponse[]> {
  const response = await apiClient.get<TaskResponse[]>(
    `/lists/${listId}/tasks`,
  );
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

export async function createTask(
  listId: string,
  request: CreateTaskRequest,
): Promise<string> {
  const response = await apiClient.post<string>(
    `/lists/${listId}/tasks`,
    request,
  );
  return response.data;
}

export async function deleteTask(taskId: string): Promise<void> {
  await apiClient.delete(`/tasks/${taskId}`);
}
