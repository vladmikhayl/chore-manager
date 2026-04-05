import { apiClient } from "./apiClient";
import type {
  NotificationSettingsRequest,
  ProfileResponse,
  TelegramLinkResponse,
} from "../types/profile";

export async function getProfile(): Promise<ProfileResponse> {
  const response = await apiClient.get<ProfileResponse>("/me/profile");
  return response.data;
}

export async function getTelegramLink(): Promise<TelegramLinkResponse> {
  const response =
    await apiClient.get<TelegramLinkResponse>("/me/telegram-link");
  return response.data;
}

export async function createTelegramLinkToken(): Promise<string> {
  const response = await apiClient.post<string>("/me/telegram/link-token");
  return response.data;
}

export async function updateNotificationSettings(
  request: NotificationSettingsRequest,
): Promise<void> {
  await apiClient.put("/me/notification-settings", request);
}

export async function deleteTelegramLink(): Promise<void> {
  await apiClient.delete("/me/telegram-link");
}
