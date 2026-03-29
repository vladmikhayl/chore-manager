import { apiClient } from "./apiClient";
import type { ProfileResponse } from "../types/profile";

export async function getProfile(): Promise<ProfileResponse> {
  const response = await apiClient.get<ProfileResponse>("/me/profile");
  return response.data;
}
